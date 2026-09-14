package com.monkey.settlement.bsm.biz.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.account.bsm.biz.request.SettleCarrierMoneyAccountRequest;
import com.monkey.ams.common.constants.UserTypeEnum;
import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.StringGenerateUtil;
import com.monkey.common.mq.core.RabbitMqProducer;
import com.monkey.settlement.bsm.biz.constants.SettlementConstants;
import com.monkey.settlement.bsm.biz.dto.SettlementApplyResultDto;
import com.monkey.settlement.bsm.biz.dto.SettlementCarrierDto;
import com.monkey.settlement.bsm.biz.dto.SettlementShipperDto;
import com.monkey.settlement.bsm.biz.entity.SettlementCarrier;
import com.monkey.settlement.bsm.biz.entity.SettlementShipper;
import com.monkey.settlement.bsm.biz.mapper.SettlementShipperMapper;
import com.monkey.settlement.bsm.biz.request.SettlementApplyRequest;
import com.monkey.settlement.bsm.biz.service.inf.SettlementCarrierService;
import com.monkey.settlement.bsm.biz.service.inf.SettlementService;
import com.monkey.settlement.bsm.biz.service.inf.SettlementShipperService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static com.monkey.ams.common.constants.AmsRabbitConstants.SETTLEMENT_DELAY_ROUTING_KEY;
import static com.monkey.common.mq.constants.RabbitConstants.SETTLEMENT_DELAY_EXCHANGE;

/**
 * 清算单业务服务实现
 * <p>
 * 结算申请建单策略（一致性 / 幂等性 / 安全性 / 并发）：
 * <p>
 * 1) 一致性：两张清算单在同一本地事务内落库，杜绝"只有货主单没有承运方单"的中间态；
 * 若历史数据只存在其中一张（极端情况），本次会幂等补齐缺失的那张，不会重复创建；
 * <p>
 * 2) 幂等性：以运单号为业务幂等键，入口先按 uk_order_id 查询已存在单据；
 * 数据库 uk_order_id 唯一索引做最终兜底，重复调用只会返回既有单号；
 * <p>
 * 3) 安全性：金额（服务费、应付总额、实收金额）一律由服务端按运单运费与服务费率计算，
 * 调用方传入的金额结果不被采信；费率仅接受 [0,1) 区间，否则拒绝建单；
 * 单号由服务端生成（前缀 + 时间戳 + 随机），不接受外部指定；
 * <p>
 * 4) 高性能/高并发：建单前仅一次唯一索引查询，写库为两条单行 INSERT，
 * 无 SELECT ... FOR UPDATE 长事务；并发请求由协议层分布式锁串行化，
 * 极端穿锁场景由唯一索引兜底。
 *
 * @author gkk
 * @since 2026-09-12
 */
@Slf4j
@Service
public class SettlementServiceImpl implements SettlementService {

    @Resource
    private RabbitMqProducer rabbitMqProducer;

    @DubboReference
    private AccountProtocol accountProtocol;

    @Resource
    private SettlementShipperService settlementShipperService;

    @Resource
    private SettlementCarrierService settlementCarrierService;

    /**
     * 货主清算单 Mapper：用于幂等命中时对缺失的托管冻结流水号做条件补齐
     */
    @Resource
    private SettlementShipperMapper settlementShipperMapper;

    /**
     * 承运方承担的服务费率（默认 5%），可由配置中心覆盖
     */
    @Value("${settlement.carrier.service-fee-rate:0.0010}")
    private BigDecimal defaultCarrierServiceFeeRate;

    /**
     * 货主承担的服务费率（默认 0，即不向货主收取服务费），可由配置中心覆盖
     */
    @Value("${settlement.shipper.service-fee-rate:0.0010}")
    private BigDecimal defaultShipperServiceFeeRate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementApplyResultDto applySettlement(SettlementApplyRequest request) {
        String orderId = request.getOrderId();
        LocalDateTime now = LocalDateTime.now();

        // 1. 幂等查询：按运单号（uk_order_id）定位两侧单据，命中则直接返回既有单号
        SettlementShipper shipper = queryShipperEntity(orderId);
        SettlementCarrier carrier = queryCarrierEntity(orderId);
        if (shipper != null && carrier != null) {
            // 幂等命中：早期（或账户域降级时）生成的单据可能缺失托管冻结流水号，按账户侧最新结果补空自愈
            shipper = supplementShipperFrozenInfo(shipper, request);
            log.info("清算单已存在，命中幂等: orderId={}, shipperNo={}, carrierNo={}, frozenNo={}",
                    orderId, shipper.getSettlementNo(), carrier.getSettlementNo(), shipper.getFrozenNo());
            return buildResult(shipper, carrier, true);
        }

        // 2. 金额计算：服务费率、金额全部由服务端计算，不采信调用方传入的金额结果
        BigDecimal transportMoney = scale(request.getTransportMoney());
        BigDecimal carrierServiceFeeRate = resolveRate(request.getCarrierServiceFeeRate(),
                defaultCarrierServiceFeeRate, "承运方");
        BigDecimal shipperServiceFeeRate = resolveRate(request.getShipperServiceFeeRate(),
                defaultShipperServiceFeeRate, "货主");

        // 承运方（应收）：实收 = 运费 - 平台服务费
        BigDecimal carrierServiceFee = scale(transportMoney.multiply(carrierServiceFeeRate));
        BigDecimal settleAmount = scale(transportMoney.subtract(carrierServiceFee));
        if (settleAmount.signum() < 0) {
            throw new IllegalArgumentException("运单运费不足以抵扣平台服务费，无法生成清算单");
        }
        // 货主（应付）：应付总额 = 运费 + 货主承担服务费（默认 0）
        BigDecimal shipperServiceFee = scale(transportMoney.multiply(shipperServiceFeeRate));
        BigDecimal payableAmount = scale(transportMoney.add(shipperServiceFee));
        BigDecimal frozenAmount = request.getFrozenAmount() == null
                ? transportMoney : scale(request.getFrozenAmount());

        String operator = request.getOperator();

        // 3. 落库：缺失的单据才创建（同一事务，保证双单原子）
        if (shipper == null) {
            shipper = buildShipper(request, now, transportMoney, shipperServiceFeeRate,
                    shipperServiceFee, payableAmount, frozenAmount, operator);
            settlementShipperService.save(shipper);
            log.info("货主清算单生成成功: orderId={}, settlementNo={}, payableAmount={}, frozenAmount={}",
                    orderId, shipper.getSettlementNo(), payableAmount, frozenAmount);

            //货主清算工单生成后，在这里做货主清算功能，走延迟队列，延迟60S
            JSONObject data = new JSONObject();
            data.put("settlementNo", shipper.getSettlementNo());
            data.put("orderId", shipper.getOrderId());
            data.put("userType", UserTypeEnum.SHIPPER.getValue());
            rabbitMqProducer.send(SETTLEMENT_DELAY_EXCHANGE,SETTLEMENT_DELAY_ROUTING_KEY, data);
        }
        if (carrier == null) {
            carrier = buildCarrier(request, now, transportMoney, carrierServiceFeeRate,
                    carrierServiceFee, settleAmount, operator);
            settlementCarrierService.save(carrier);
            log.info("承运方清算单生成成功: orderId={}, settlementNo={}, settleAmount={}, serviceFee={}",
                    orderId, carrier.getSettlementNo(), settleAmount, carrierServiceFee);
            //承运方清算工单生成后，在这里做承运方清算功能，走延迟队列，延迟30S
            /*JSONObject data = new JSONObject();
            data.put("settlementNo", carrier.getSettlementNo());
            data.put("orderId", carrier.getOrderId());
            data.put("userType", UserTypeEnum.DRIVER.getValue());
            rabbitMqProducer.send(SETTLEMENT_DELAY_EXCHANGE,SETTLEMENT_DELAY_ROUTING_KEY, data);*/
        }
        return buildResult(shipper, carrier, false);
    }

    @Override
    public SettlementShipperDto queryShipperSettlement(String orderId) {
        SettlementShipper shipper = queryShipperEntity(orderId);
        return shipper == null ? null : toShipperDto(shipper);
    }

    @Override
    public SettlementCarrierDto queryCarrierSettlement(String orderId) {
        SettlementCarrier carrier = queryCarrierEntity(orderId);
        return carrier == null ? null : toCarrierDto(carrier);
    }

    /**
     * 承运方对账：平台公司对公账户 -> 承运方账户划账
     * <p>
     * 资金方向（与货主清算分离）：
     * 1) 货主清算（独立 MQ 流程）：货主托管冻结运费 -> 平台公司对公账户；
     * 2) 承运方对账（本方法）：平台公司对公账户 -> 承运方智运宝账户，不从货主侧再扣减任何资金。
     * <p>
     * 幂等与并发：
     * 1) 承运方清算单已结算 -> 幂等返回成功；
     * 2) 待结算 -> 结算中 CAS 抢占，防并发重复划账（结算中表示上一次执行中断，账户侧按清算单号幂等，可安全续跑）；
     * 3) 账户侧以清算单号为幂等锚点落地划账流水，重复请求不会重复出账。
     *
     * @param orderId  运单号
     * @param operator 操作人（承运方用户ID）
     * @return 对账结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result reconcileOrder(String orderId, String operator) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return Result.fail("运单号为空");
        }
        if (operator == null || operator.trim().isEmpty()) {
            return Result.fail("操作人为空");
        }

        // 1. 查询两侧清算单（uk_order_id 唯一索引）
        SettlementShipper shipper = queryShipperEntity(orderId);
        SettlementCarrier carrier = queryCarrierEntity(orderId);
        if (carrier == null) {
            log.warn("对账失败：承运方清算单不存在, orderId={}", orderId);
            return Result.fail("承运方清算单不存在，无法对账");
        }

        // 2. 幂等命中：承运方清算单已结算，资金已划付，直接返回成功
        if (SettlementConstants.STATUS_SETTLED == carrier.getStatus()) {
            log.info("对账幂等命中：承运方清算单已结算, orderId={}, carrierSettlementNo={}",
                    orderId, carrier.getSettlementNo());
            return Result.success();
        }

        // 3. 前置校验：承运方款项由平台公司对公账户划付，货主清算未完成时平台账户尚无该运单资金，
        //    此处拒绝划账，避免平台垫付与资金悬空
        if (shipper == null || shipper.getStatus() == null
                || SettlementConstants.STATUS_SETTLED != shipper.getStatus()) {
            log.warn("对账失败：货主清算未完成, orderId={}, shipperStatus={}",
                    orderId, shipper == null ? null : shipper.getStatus());
            return Result.fail("货主清算未完成，平台公司对公账户尚无该运单资金，暂不可向承运方划账");
        }

        // 4. 状态校验：仅「待结算(1)」与新「结算中(2)」（上次执行中断可续跑）允许对账
        Byte carrierStatus = carrier.getStatus();
        if (SettlementConstants.STATUS_PENDING != carrierStatus && SettlementConstants.STATUS_SETTLING != carrierStatus) {
            log.warn("对账失败：承运方清算单状态异常, orderId={}, carrierStatus={}, statusDesc={}",
                    orderId, carrierStatus, carrier.getStatusDesc());
            return Result.fail("承运方清算单状态异常，无法对账");
        }

        // 5. 金额安全校验：划账金额取清算单实收金额，异常金额绝不发起资金动作
        BigDecimal settleAmount = carrier.getSettleAmount();
        if (settleAmount == null || settleAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("对账失败：承运方实收金额异常, orderId={}, settleAmount={}", orderId, settleAmount);
            return Result.fail("承运方实收金额异常，拒绝划账");
        }
        if (settleAmount.compareTo(SettlementConstants.MAX_TRANSPORT_MONEY) > 0) {
            log.warn("对账失败：承运方实收金额超出合理范围, orderId={}, settleAmount={}", orderId, settleAmount);
            return Result.fail("承运方实收金额超出合理范围，拒绝划账");
        }

        // 6. 状态推进：待结算(1) -> 结算中(2)，CAS 防并发重复划账
        if (SettlementConstants.STATUS_PENDING == carrierStatus) {
            boolean advanced = settlementCarrierService.lambdaUpdate()
                    .set(SettlementCarrier::getStatus, SettlementConstants.STATUS_SETTLING)
                    .set(SettlementCarrier::getStatusDesc, SettlementConstants.STATUS_DESC_SETTLING)
                    .set(SettlementCarrier::getUpdateTime, LocalDateTime.now())
                    .set(SettlementCarrier::getUpdateName, operator)
                    .eq(SettlementCarrier::getId, carrier.getId())
                    .eq(SettlementCarrier::getStatus, SettlementConstants.STATUS_PENDING)
                    .update();
            if (!advanced) {
                SettlementCarrier latest = settlementCarrierService.getById(carrier.getId());
                Byte latestStatus = latest == null ? null : latest.getStatus();
                if (latestStatus != null && SettlementConstants.STATUS_SETTLED == latestStatus) {
                    // 已被并发请求划账完成，幂等返回
                    log.info("对账幂等命中：承运方清算单已被并发请求结算, orderId={}, carrierSettlementNo={}",
                            orderId, carrier.getSettlementNo());
                    return Result.success();
                }
                if (latestStatus == null || SettlementConstants.STATUS_SETTLING != latestStatus) {
                    log.warn("对账失败：承运方清算单状态已被并发修改, orderId={}, latestStatus={}",
                            orderId, latestStatus);
                    return Result.fail("承运方清算单状态已被并发修改，请稍后重试");
                }
                // 已是「结算中」：上一次执行中断，账户侧按清算单号幂等，继续执行安全
            }
        }

        // 7. 账户侧资金动作：平台公司对公账户 -> 承运方账户
        //    （出账账户由账户服务配置指定，不接受调用方传入；划账金额取清算单实收金额）
        SettleCarrierMoneyAccountRequest accountRequest = new SettleCarrierMoneyAccountRequest();
        accountRequest.setCarrierUserId(carrier.getCarrierUserId());
        accountRequest.setSettlementNo(carrier.getSettlementNo());
        accountRequest.setOrderNo(orderId);
        accountRequest.setSettleAmount(settleAmount);
        accountRequest.setOperator(operator);

        Result<BigDecimal> accountResult = accountProtocol.settleCarrierMoneyFromCompanyAccount(accountRequest);
        if (accountResult == null || !accountResult.isSuccess()) {
            String message = accountResult == null ? "账户服务无响应" : accountResult.getMessage();
            log.warn("对账失败：账户侧划账未成功, orderId={}, msg={}", orderId, message);
            return Result.fail("资金划账失败：" + message);
        }
        BigDecimal paidAmount = accountResult.getData() == null ? settleAmount : accountResult.getData();

        // 8. 回写承运方清算单为已结算（CAS：待结算/结算中 -> 已结算，避免并发覆盖）
        LocalDateTime now = LocalDateTime.now();
        boolean carrierUpdated = settlementCarrierService.lambdaUpdate()
                .set(SettlementCarrier::getStatus, SettlementConstants.STATUS_SETTLED)
                .set(SettlementCarrier::getStatusDesc, SettlementConstants.STATUS_DESC_SETTLED)
                .set(SettlementCarrier::getPayTime, now)
                .set(SettlementCarrier::getUpdateTime, now)
                .set(SettlementCarrier::getUpdateName, operator)
                .eq(SettlementCarrier::getId, carrier.getId())
                .in(SettlementCarrier::getStatus,
                        SettlementConstants.STATUS_PENDING, SettlementConstants.STATUS_SETTLING)
                .update();
        if (!carrierUpdated) {
            // 账户侧已按清算单号幂等，不会重复出账；此处只需确认是否已被并发请求置为已结算
            SettlementCarrier latest = settlementCarrierService.getById(carrier.getId());
            if (latest != null && latest.getStatus() != null
                    && SettlementConstants.STATUS_SETTLED == latest.getStatus()) {
                log.info("承运方清算单已由并发请求置为已结算，对账按成功收口: orderId={}, carrierSettlementNo={}",
                        orderId, carrier.getSettlementNo());
                return Result.success();
            }
            log.error("承运方清算单回写失败，需人工核对资金与单据: orderId={}, carrierSettlementNo={}",
                    orderId, carrier.getSettlementNo());
            return Result.fail("承运方清算单回写失败，请稍后重试");
        }

        log.info("对账成功（平台公司对公账户 -> 承运方账户）: orderId={}, carrierSettlementNo={}, carrierUserId={}, settleAmount={}, operator={}",
                orderId, carrier.getSettlementNo(), carrier.getCarrierUserId(), paidAmount, operator);
        return Result.success();
    }

    /**
     * 幂等命中的自愈补齐：早期（或账户域降级时）生成的货主清算单可能缺失托管冻结流水号，
     * 本次若已从账户侧取到流水号，则在「待结算 + 流水号仍为空」条件下做一次条件更新补齐。
     * <p>
     * 安全约束：
     * 1) 仅 status=1（待结算，尚未发生任何资金动作）允许补齐，已结算/已作废单据不被改动；
     * 2) 更新条件带主键与 status=1，且仅在内存判定「流水号仍为空」时才执行；外层按运单号分布式锁串行化，
     *    极端并发同时补写时写入值同源（均取自账户侧同一冻结记录），不会产生数据漂移；
     * 3) 仅补 frozen_no 与账户侧实际托管金额，不触碰应付/实付等结算口径与状态字段。
     *
     * @param shipper 已存在的货主清算单
     * @param request 本次申请参数（已由协议层从账户侧补全 frozenNo / frozenAmount）
     * @return 补齐后的货主清算单（无需补齐时原样返回）
     */
    private SettlementShipper supplementShipperFrozenInfo(SettlementShipper shipper, SettlementApplyRequest request) {
        String frozenNo = request.getFrozenNo();
        boolean shipperFrozenNoBlank = shipper.getFrozenNo() == null || shipper.getFrozenNo().trim().isEmpty();
        boolean requestFrozenNoBlank = frozenNo == null || frozenNo.trim().isEmpty();
        boolean pending = shipper.getStatus() != null
                && shipper.getStatus() == SettlementConstants.STATUS_PENDING;
        if (!shipperFrozenNoBlank || requestFrozenNoBlank || !pending) {
            return shipper;
        }
        BigDecimal frozenAmount = request.getFrozenAmount() == null
                ? shipper.getFrozenAmount() : scale(request.getFrozenAmount());
        String operator = request.getOperator();
        boolean hasOperator = operator != null && !operator.trim().isEmpty();
        int rows = settlementShipperMapper.update(null, Wrappers.<SettlementShipper>lambdaUpdate()
                .set(SettlementShipper::getFrozenNo, frozenNo)
                .set(SettlementShipper::getFrozenAmount, frozenAmount)
                .set(SettlementShipper::getUpdateTime, LocalDateTime.now())
                .set(hasOperator, SettlementShipper::getUpdateName, operator)
                .eq(SettlementShipper::getId, shipper.getId())
                .eq(SettlementShipper::getStatus, SettlementConstants.STATUS_PENDING));
        if (rows == 1) {
            shipper.setFrozenNo(frozenNo);
            shipper.setFrozenAmount(frozenAmount);
            log.info("货主清算单托管冻结流水号已自愈补齐: orderId={}, settlementNo={}, frozenNo={}, frozenAmount={}",
                    shipper.getOrderId(), shipper.getSettlementNo(), frozenNo, frozenAmount);
        } else {
            log.info("货主清算单托管冻结流水号无需补齐（已被并发请求补齐或状态已变化）: orderId={}, settlementNo={}",
                    shipper.getOrderId(), shipper.getSettlementNo());
        }
        return shipper;
    }

    /**
     * 按运单号查询货主清算单（走 uk_order_id 唯一索引，最多 1 条）
     */
    private SettlementShipper queryShipperEntity(String orderId) {
        return settlementShipperService.lambdaQuery()
                .eq(SettlementShipper::getOrderId, orderId)
                .eq(SettlementShipper::getDeleteFlag, SettlementConstants.DELETE_FLAG_NO)
                .last("limit 1")
                .one();
    }

    /**
     * 按运单号查询承运方清算单（走 uk_order_id 唯一索引，最多 1 条）
     */
    private SettlementCarrier queryCarrierEntity(String orderId) {
        return settlementCarrierService.lambdaQuery()
                .eq(SettlementCarrier::getOrderId, orderId)
                .eq(SettlementCarrier::getDeleteFlag, SettlementConstants.DELETE_FLAG_NO)
                .last("limit 1")
                .one();
    }

    /**
     * 货主清算单构建：申请阶段仅固化应付口径，实付/退款均为 0，实际扣划在「结算(8)」阶段回写
     */
    private SettlementShipper buildShipper(SettlementApplyRequest request, LocalDateTime now,
                                           BigDecimal transportMoney, BigDecimal serviceFeeRate,
                                           BigDecimal serviceFee, BigDecimal payableAmount,
                                           BigDecimal frozenAmount, String operator) {
        SettlementShipper shipper = new SettlementShipper();
        shipper.setSettlementNo(StringGenerateUtil.generateOrderNo(SettlementConstants.SHIPPER_SETTLEMENT_NO_PREFIX));
        shipper.setOrderId(request.getOrderId());
        shipper.setShipperUserId(request.getShipperUserId());
        shipper.setShipperUserName(request.getShipperUserName());
        shipper.setShipperName(request.getShipperName());
        shipper.setShipperMobile(request.getShipperMobile());
        shipper.setCarrierUserId(request.getCarrierUserId());
        shipper.setCarrierName(request.getCarrierName());
        shipper.setCarrierMobile(request.getCarrierMobile());
        shipper.setTransportMoney(transportMoney);
        shipper.setServiceFeeRate(serviceFeeRate);
        shipper.setServiceFee(serviceFee);
        shipper.setDiscountAmount(BigDecimal.ZERO.setScale(SettlementConstants.AMOUNT_SCALE, RoundingMode.HALF_UP));
        shipper.setOtherAmount(BigDecimal.ZERO.setScale(SettlementConstants.AMOUNT_SCALE, RoundingMode.HALF_UP));
        shipper.setPayableAmount(payableAmount);
        shipper.setFrozenNo(request.getFrozenNo());
        shipper.setFrozenAmount(frozenAmount);
        // 申请阶段不产生资金动作，实付与退款为 0，结算(8) 阶段由 account 侧扣划后回写
        shipper.setPaidAmount(BigDecimal.ZERO.setScale(SettlementConstants.AMOUNT_SCALE, RoundingMode.HALF_UP));
        shipper.setRefundAmount(BigDecimal.ZERO.setScale(SettlementConstants.AMOUNT_SCALE, RoundingMode.HALF_UP));
        shipper.setStatus(SettlementConstants.STATUS_PENDING);
        shipper.setStatusDesc(SettlementConstants.STATUS_DESC_PENDING);
        shipper.setInvoiceStatus(SettlementConstants.INVOICE_STATUS_NONE);
        shipper.setApplyTime(now);
        shipper.setRemark(request.getRemark());
        shipper.setDeleteFlag(SettlementConstants.DELETE_FLAG_NO);
        shipper.setCreateTime(now);
        shipper.setCreateName(operator);
        shipper.setUpdateTime(now);
        shipper.setUpdateName(operator);
        return shipper;
    }

    /**
     * 承运方清算单构建：申请阶段固化应收口径，打款时间在「结算(8)」阶段回写
     */
    private SettlementCarrier buildCarrier(SettlementApplyRequest request, LocalDateTime now,
                                           BigDecimal transportMoney, BigDecimal serviceFeeRate,
                                           BigDecimal serviceFee, BigDecimal settleAmount, String operator) {
        SettlementCarrier carrier = new SettlementCarrier();
        carrier.setSettlementNo(StringGenerateUtil.generateOrderNo(SettlementConstants.CARRIER_SETTLEMENT_NO_PREFIX));
        carrier.setOrderId(request.getOrderId());
        carrier.setShipperUserId(request.getShipperUserId());
        carrier.setShipperName(request.getShipperName());
        carrier.setShipperMobile(request.getShipperMobile());
        carrier.setCarrierUserId(request.getCarrierUserId());
        carrier.setCarrierUserName(request.getCarrierUserName());
        carrier.setCarrierName(request.getCarrierName());
        carrier.setCarrierMobile(request.getCarrierMobile());
        carrier.setTransportMoney(transportMoney);
        carrier.setServiceFeeRate(serviceFeeRate);
        carrier.setServiceFee(serviceFee);
        carrier.setTaxAmount(BigDecimal.ZERO.setScale(SettlementConstants.AMOUNT_SCALE, RoundingMode.HALF_UP));
        carrier.setOtherAmount(BigDecimal.ZERO.setScale(SettlementConstants.AMOUNT_SCALE, RoundingMode.HALF_UP));
        carrier.setDeductibleAmount(BigDecimal.ZERO.setScale(SettlementConstants.AMOUNT_SCALE, RoundingMode.HALF_UP));
        carrier.setSettleAmount(settleAmount);
        // 发货保证金在「回单确认(6)」已解冻退回，仅作展示核算，不参与实收计算，故此处不落值
        carrier.setStatus(SettlementConstants.STATUS_PENDING);
        carrier.setStatusDesc(SettlementConstants.STATUS_DESC_PENDING);
        carrier.setInvoiceStatus(SettlementConstants.INVOICE_STATUS_NONE);
        carrier.setApplyTime(now);
        carrier.setRemark(request.getRemark());
        carrier.setDeleteFlag(SettlementConstants.DELETE_FLAG_NO);
        carrier.setCreateTime(now);
        carrier.setCreateName(operator);
        carrier.setUpdateTime(now);
        carrier.setUpdateName(operator);
        return carrier;
    }

    private SettlementApplyResultDto buildResult(SettlementShipper shipper, SettlementCarrier carrier, boolean existed) {
        SettlementApplyResultDto result = new SettlementApplyResultDto();
        result.setShipperSettlementId(shipper.getId());
        result.setShipperSettlementNo(shipper.getSettlementNo());
        result.setPayableAmount(shipper.getPayableAmount());
        result.setFrozenAmount(shipper.getFrozenAmount());
        result.setCarrierSettlementId(carrier.getId());
        result.setCarrierSettlementNo(carrier.getSettlementNo());
        result.setSettleAmount(carrier.getSettleAmount());
        result.setCarrierServiceFee(carrier.getServiceFee());
        result.setExisted(existed);
        return result;
    }

    /**
     * 费率决策：优先取调用方传入的约定费率，缺省使用服务端配置；越界直接拒绝建单
     */
    private BigDecimal resolveRate(BigDecimal rate, BigDecimal defaultRate, String subject) {
        BigDecimal target = rate == null ? defaultRate : rate;
        if (target == null) {
            target = BigDecimal.ZERO;
        }
        if (target.signum() < 0 || target.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException(subject + "服务费率必须在 [0,1) 区间内");
        }
        return target;
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount == null ? null : amount.setScale(SettlementConstants.AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private SettlementShipperDto toShipperDto(SettlementShipper entity) {
        SettlementShipperDto dto = new SettlementShipperDto();
        BeanUtils.copyProperties(entity, dto, "applyTime", "settleTime", "createTime", "updateTime");
        dto.setApplyTime(toDate(entity.getApplyTime()));
        dto.setSettleTime(toDate(entity.getSettleTime()));
        dto.setCreateTime(toDate(entity.getCreateTime()));
        dto.setUpdateTime(toDate(entity.getUpdateTime()));
        return dto;
    }

    private SettlementCarrierDto toCarrierDto(SettlementCarrier entity) {
        SettlementCarrierDto dto = new SettlementCarrierDto();
        BeanUtils.copyProperties(entity, dto, "applyTime", "settleTime", "payTime", "createTime", "updateTime");
        dto.setApplyTime(toDate(entity.getApplyTime()));
        dto.setSettleTime(toDate(entity.getSettleTime()));
        dto.setPayTime(toDate(entity.getPayTime()));
        dto.setCreateTime(toDate(entity.getCreateTime()));
        dto.setUpdateTime(toDate(entity.getUpdateTime()));
        return dto;
    }

    private Date toDate(LocalDateTime localDateTime) {
        return localDateTime == null ? null : Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
