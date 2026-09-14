package com.monkey.settlement.bsm.biz.service.impl;

import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.account.bsm.biz.request.SettleShipperMoneyAccountRequest;
import com.monkey.ams.common.constants.AmsRabbitConstants;
import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.StringGenerateUtil;
import com.monkey.common.lock.annotation.DistributedLock;
import com.monkey.common.mq.constants.RabbitConstants;
import com.monkey.settlement.bsm.biz.constants.SettlementConstants;
import com.monkey.settlement.bsm.biz.constants.SettlementMqConstants;
import com.monkey.settlement.bsm.biz.entity.SettlementShipper;
import com.monkey.settlement.bsm.biz.entity.SettlementShipperWorkOrder;
import com.monkey.settlement.bsm.biz.service.inf.SettlementShipperExecuteService;
import com.monkey.settlement.bsm.biz.service.inf.SettlementShipperService;
import com.monkey.settlement.bsm.biz.service.inf.SettlementShipperWorkOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 货主清算执行（MQ 消费端）
 * <p>
 * 执行顺序严格遵循「先落地工单 -> 再做业务」：
 * 1) 落地 tf_b_settlement_shipper_work_order 工单（消费幂等锚点，唯一键兜底）；
 * 2) 工单状态机 CAS 抢占（防并发重复消费）；
 * 3) 货主清算：清算单「待结算→结算中」推进 -> 账户侧冻结扣划至平台公司对公账户 -> 清算单置「已结算」；
 * 4) 工单收口为「处理成功」或「处理失败（登记重试）」。
 * <p>
 * 业务边界：本类只处理货主侧资金，不做承运方任何入账（承运方结算单独处理）。
 *
 * @author gkk
 * @since 2026-09-14
 */
@Slf4j
@Service
public class SettlementShipperExecuteServiceImpl implements SettlementShipperExecuteService {

    /**
     * 系统操作人（MQ 异步清算无登录态）
     */
    private static final String SYSTEM_OPERATOR = "system";

    /**
     * 幂等键最大长度（tf_b_settlement_shipper_work_order.idempotent_key varchar(96)）
     */
    private static final int MAX_IDEMPOTENT_KEY_LENGTH = 96;

    /**
     * 消息体快照最大长度（payload 字段容量有限，超长截断保证落库成功）
     */
    private static final int MAX_PAYLOAD_LENGTH = 2000;

    /**
     * 失败原因最大长度（error_msg varchar(512)）
     */
    private static final int MAX_ERROR_MSG_LENGTH = 500;

    @Resource
    private SettlementShipperService settlementShipperService;

    @Resource
    private SettlementShipperWorkOrderService settlementShipperWorkOrderService;

    @DubboReference
    private AccountProtocol accountProtocol;

    @DistributedLock(key = "'settlement:shipper:execute:' + #settlementNo", waitTime = 3, leaseTime = -1)
    @Override
    public Result executeSettlementShipper(String settlementNo, String orderId, String messageId, String payload) {
        // ==================== 1. 入口校验：畸形/被篡改消息直接拒绝，不落脏数据 ====================
        String invalid = validateParam(settlementNo, orderId);
        if (invalid != null) {
            log.warn("货主清算消息参数非法，拒绝执行: settlementNo={}, orderId={}, messageId={}, reason={}",
                    settlementNo, orderId, messageId, invalid);
            return Result.fail(invalid);
        }
        String clearSettlementNo = settlementNo.trim();
        String clearOrderId = orderId.trim();

        // ==================== 2. 幂等第一重：先落地货主清算 MQ 工单 ====================
        SettlementShipperWorkOrder workOrder;
        try {
            workOrder = landWorkOrder(clearSettlementNo, clearOrderId, messageId, payload);
        } catch (Exception e) {
            log.error("货主清算工单落地异常: settlementNo={}, orderId={}, messageId={}",
                    clearSettlementNo, clearOrderId, messageId, e);
            return Result.fail("清算工单落地失败，请稍后重试");
        }
        if (workOrder == null) {
            return Result.fail("清算工单落地失败，请稍后重试");
        }

        // ==================== 3. 幂等第二重：工单已处理成功，直接幂等返回 ====================
        if (isWorkOrderSuccess(workOrder)) {
            log.info("货主清算工单已处理成功，重复消费幂等跳过: settlementNo={}, workOrderNo={}",
                    clearSettlementNo, workOrder.getWorkOrderNo());
            return Result.success();
        }

        // ==================== 4. 幂等第三重：CAS 抢占工单（待投递/已投递/处理失败 -> 处理中） ====================
        if (isRetryExhausted(workOrder)) {
            log.error("货主清算工单重试次数已达上限，跳过自动清算等待人工介入: settlementNo={}, workOrderNo={}, retryCount={}",
                    clearSettlementNo, workOrder.getWorkOrderNo(), workOrder.getRetryCount());
            return Result.success();
        }
        if (!tryAcquireWorkOrder(workOrder)) {
            log.info("货主清算工单已被其他消费者抢占或状态不可执行，重复消费幂等跳过: settlementNo={}, workOrderNo={}",
                    clearSettlementNo, workOrder.getWorkOrderNo());
            return Result.success();
        }

        // ==================== 5. 货主清算业务：冻结扣划 -> 平台公司对公账户 ====================
        try {
            Result<String> businessResult = settleShipperMoney(workOrder);
            if (businessResult != null && businessResult.isSuccess()) {
                markWorkOrderSuccess(workOrder);
                return Result.success();
            }
            String businessError = businessResult == null ? "货主清算执行失败" : businessResult.getMessage();
            markWorkOrderFail(workOrder, businessError);
            return Result.fail(businessError);
        } catch (Exception e) {
            log.error("货主清算执行异常: settlementNo={}, orderId={}, workOrderNo={}",
                    clearSettlementNo, clearOrderId, workOrder.getWorkOrderNo(), e);
            markWorkOrderFail(workOrder, "货主清算执行异常：" + e.getMessage());
            return Result.fail("货主清算执行异常，请稍后重试");
        }
    }

    // ==================================================================================
    // 工单落地 / 幂等占位
    // ==================================================================================

    /**
     * 落地货主清算 MQ 工单（幂等）
     * <p>
     * 1) 先按唯一键 settlement_no 回查，命中即复用（生产者已建单或重复消费场景）；
     * 2) 未命中则插入工单，status=已投递，幂等键 = SHIPPER_CLEARING:{orderId}；
     * 3) 并发插入由 uk_settlement_no / uk_idempotent_key 兜底，撞唯一键后回查复用，绝不让重复消息产生第二条工单。
     */
    private SettlementShipperWorkOrder landWorkOrder(String settlementNo, String orderId, String messageId, String payload) {
        SettlementShipperWorkOrder existed = queryWorkOrder(settlementNo);
        if (existed != null) {
            return existed;
        }
        // 冗余快照（只读清算单，不做任何状态与资金动作），便于排障与消息重放
        SettlementShipper shipper = queryShipper(settlementNo);

        LocalDateTime now = LocalDateTime.now();
        SettlementShipperWorkOrder workOrder = new SettlementShipperWorkOrder();
        workOrder.setWorkOrderNo(StringGenerateUtil.generateOrderNo(SettlementConstants.SHIPPER_WORK_ORDER_NO_PREFIX));
        workOrder.setSettlementNo(settlementNo);
        workOrder.setOrderId(orderId);
        workOrder.setIdempotentKey(buildIdempotentKey(orderId));
        if (shipper != null) {
            workOrder.setShipperUserId(shipper.getShipperUserId());
            workOrder.setShipperUserName(shipper.getShipperUserName());
            workOrder.setShipperName(shipper.getShipperName());
            workOrder.setShipperMobile(shipper.getShipperMobile());
            workOrder.setCarrierUserId(shipper.getCarrierUserId());
            workOrder.setCarrierName(shipper.getCarrierName());
            workOrder.setCarrierMobile(shipper.getCarrierMobile());
            workOrder.setTransportMoney(shipper.getTransportMoney());
            workOrder.setPayableAmount(shipper.getPayableAmount());
            workOrder.setFrozenNo(shipper.getFrozenNo());
            workOrder.setFrozenAmount(shipper.getFrozenAmount());
        }
        // MQ 元数据（投递入口：延迟交换机 -> 死信交换机 -> 执行队列）
        workOrder.setExchangeName(RabbitConstants.SETTLEMENT_DELAY_EXCHANGE);
        workOrder.setRoutingKey(AmsRabbitConstants.SETTLEMENT_DELAY_ROUTING_KEY);
        workOrder.setQueueName(SettlementMqConstants.SETTLEMENT_EXECUTE_QUEUE);
        workOrder.setMessageId(isBlank(messageId) ? UUID.randomUUID().toString().replace("-", "") : messageId);
        workOrder.setMessageType(SettlementConstants.MESSAGE_TYPE_SHIPPER_CLEARING);
        workOrder.setBusinessId(orderId);
        workOrder.setPayload(truncate(payload, MAX_PAYLOAD_LENGTH));
        workOrder.setDelaySeconds(SettlementConstants.WORK_ORDER_DELAY_SECONDS);
        workOrder.setStatus(SettlementConstants.WORK_ORDER_STATUS_SENT);
        workOrder.setStatusDesc(SettlementConstants.WORK_ORDER_DESC_SENT);
        workOrder.setRetryCount(0);
        workOrder.setMaxRetryCount(SettlementConstants.WORK_ORDER_MAX_RETRY_COUNT);
        workOrder.setSendTime(now);
        workOrder.setConsumeTime(now);
        workOrder.setRemark("清算消息已到达执行队列，消费端落地工单");
        workOrder.setDeleteFlag(SettlementConstants.DELETE_FLAG_NO);
        workOrder.setCreateTime(now);
        workOrder.setCreateName(SYSTEM_OPERATOR);
        workOrder.setUpdateTime(now);
        workOrder.setUpdateName(SYSTEM_OPERATOR);
        try {
            settlementShipperWorkOrderService.save(workOrder);
        } catch (DuplicateKeyException e) {
            // 并发重复消费：唯一键兜底，回查既有工单继续走幂等判定
            SettlementShipperWorkOrder concurrent = queryWorkOrder(settlementNo);
            if (concurrent == null) {
                throw e;
            }
            log.info("货主清算工单并发落地命中唯一键，复用既有工单: settlementNo={}, workOrderNo={}",
                    settlementNo, concurrent.getWorkOrderNo());
            return concurrent;
        }
        return workOrder;
    }

    /**
     * CAS 抢占工单：仅当工单处于「待投递 / 已投递 / 处理失败」时允许推进到「处理中」
     *
     * @return true 表示抢占成功（当前实例持有执行权）
     */
    private boolean tryAcquireWorkOrder(SettlementShipperWorkOrder workOrder) {
        LocalDateTime now = LocalDateTime.now();
        return settlementShipperWorkOrderService.lambdaUpdate()
                .set(SettlementShipperWorkOrder::getStatus, SettlementConstants.WORK_ORDER_STATUS_PROCESSING)
                .set(SettlementShipperWorkOrder::getStatusDesc, SettlementConstants.WORK_ORDER_DESC_PROCESSING)
                .set(SettlementShipperWorkOrder::getConsumeTime, now)
                .set(SettlementShipperWorkOrder::getUpdateTime, now)
                .set(SettlementShipperWorkOrder::getUpdateName, SYSTEM_OPERATOR)
                .eq(SettlementShipperWorkOrder::getId, workOrder.getId())
                .in(SettlementShipperWorkOrder::getStatus,
                        SettlementConstants.WORK_ORDER_STATUS_WAIT_SEND,
                        SettlementConstants.WORK_ORDER_STATUS_SENT,
                        SettlementConstants.WORK_ORDER_STATUS_FAIL)
                .update();
    }

    private void markWorkOrderSuccess(SettlementShipperWorkOrder workOrder) {
        LocalDateTime now = LocalDateTime.now();
        settlementShipperWorkOrderService.lambdaUpdate()
                .set(SettlementShipperWorkOrder::getStatus, SettlementConstants.WORK_ORDER_STATUS_SUCCESS)
                .set(SettlementShipperWorkOrder::getStatusDesc, SettlementConstants.WORK_ORDER_DESC_SUCCESS)
                .set(SettlementShipperWorkOrder::getFinishTime, now)
                .set(SettlementShipperWorkOrder::getNextRetryTime, null)
                .set(SettlementShipperWorkOrder::getUpdateTime, now)
                .set(SettlementShipperWorkOrder::getUpdateName, SYSTEM_OPERATOR)
                .eq(SettlementShipperWorkOrder::getId, workOrder.getId())
                .eq(SettlementShipperWorkOrder::getStatus, SettlementConstants.WORK_ORDER_STATUS_PROCESSING)
                .update();
    }

    /**
     * 工单登记失败：状态置「处理失败」，重试次数 +1 并登记下次重试时间（供重试调度扫描补偿）
     */
    private void markWorkOrderFail(SettlementShipperWorkOrder workOrder, String errorMsg) {
        LocalDateTime now = LocalDateTime.now();
        int retryCount = workOrder.getRetryCount() == null ? 0 : workOrder.getRetryCount();
        settlementShipperWorkOrderService.lambdaUpdate()
                .set(SettlementShipperWorkOrder::getStatus, SettlementConstants.WORK_ORDER_STATUS_FAIL)
                .set(SettlementShipperWorkOrder::getStatusDesc, SettlementConstants.WORK_ORDER_DESC_FAIL)
                .set(SettlementShipperWorkOrder::getRetryCount, retryCount + 1)
                .set(SettlementShipperWorkOrder::getNextRetryTime,
                        now.plusSeconds(SettlementConstants.WORK_ORDER_RETRY_INTERVAL_SECONDS))
                .set(SettlementShipperWorkOrder::getErrorMsg, truncate(errorMsg, MAX_ERROR_MSG_LENGTH))
                .set(SettlementShipperWorkOrder::getUpdateTime, now)
                .set(SettlementShipperWorkOrder::getUpdateName, SYSTEM_OPERATOR)
                .eq(SettlementShipperWorkOrder::getId, workOrder.getId())
                .eq(SettlementShipperWorkOrder::getStatus, SettlementConstants.WORK_ORDER_STATUS_PROCESSING)
                .update();
    }

    // ==================================================================================
    // 货主清算业务（只做货主侧：冻结扣划 -> 平台公司对公账户）
    // ==================================================================================

    /**
     * 货主清算核心业务
     *
     * @return 成功（含幂等命中）返回 Result.success()；失败返回 Result.fail(失败原因)
     */
    private Result<String> settleShipperMoney(SettlementShipperWorkOrder workOrder) {
        // 1) 定位清算单，并做「工单-清算单」串单校验，防止消息被篡改后清算他人运单
        SettlementShipper shipper = queryShipper(workOrder.getSettlementNo());
        if (shipper == null) {
            return Result.fail("货主清算单不存在：" + workOrder.getSettlementNo());
        }
        if (!Objects.equals(shipper.getOrderId(), workOrder.getOrderId())) {
            return Result.fail("清算工单与清算单运单号不一致，拒绝清算");
        }

        // 2) 状态幂等：已结算直接返回成功，资金动作不会被重复触发
        Byte status = shipper.getStatus();
        if (status != null && status == SettlementConstants.STATUS_SETTLED) {
            log.info("货主清算单已结算，幂等返回成功: settlementNo={}, orderId={}",
                    shipper.getSettlementNo(), shipper.getOrderId());
            return Result.success();
        }
        if (status == null
                || (status != SettlementConstants.STATUS_PENDING && status != SettlementConstants.STATUS_SETTLING)) {
            return Result.fail("货主清算单状态异常，无法清算：" + shipper.getStatusDesc());
        }

        // 3) 金额安全校验：金额异常直接失败，绝不发起资金动作
        BigDecimal frozenAmount = shipper.getFrozenAmount();
        if (frozenAmount == null || frozenAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("货主托管冻结金额异常，拒绝清算");
        }
        if (frozenAmount.compareTo(SettlementConstants.MAX_TRANSPORT_MONEY) > 0) {
            return Result.fail("货主托管冻结金额超出合理范围，拒绝清算");
        }
        BigDecimal payableAmount = shipper.getPayableAmount() == null ? BigDecimal.ZERO : shipper.getPayableAmount();
        if (payableAmount.compareTo(SettlementConstants.MAX_TRANSPORT_MONEY) > 0) {
            return Result.fail("货主应付金额超出合理范围，拒绝清算");
        }

        // 4) 状态推进：待结算(1) -> 结算中(2)，CAS 防并发重复扣划
        if (status == SettlementConstants.STATUS_PENDING) {
            boolean advanced = casShipperStatus(shipper.getId(),
                    SettlementConstants.STATUS_PENDING, SettlementConstants.STATUS_SETTLING);
            if (!advanced) {
                SettlementShipper latest = settlementShipperService.getById(shipper.getId());
                Byte latestStatus = latest == null ? null : latest.getStatus();
                if (latestStatus != null && latestStatus == SettlementConstants.STATUS_SETTLED) {
                    // 已被并发请求清算完成，幂等返回
                    return Result.success();
                }
                if (latestStatus == null || latestStatus != SettlementConstants.STATUS_SETTLING) {
                    return Result.fail("货主清算单状态已被并发修改，请稍后重试");
                }
                // 已是「结算中」：上一次执行中断，账户侧按冻结流水号幂等，继续执行安全
            }
        }

        // 5) 账户侧资金动作：货主托管冻结运费扣划并结算至平台公司对公账户
        //    （目标账户由账户服务配置指定，不接受调用方传入；扣划金额以账户侧冻结明细为真相源）
        SettleShipperMoneyAccountRequest accountRequest = new SettleShipperMoneyAccountRequest();
        accountRequest.setShipperUserId(shipper.getShipperUserId());
        accountRequest.setOrderNo(shipper.getOrderId());
        accountRequest.setFrozenNo(shipper.getFrozenNo());
        accountRequest.setFrozenAmount(frozenAmount);
        accountRequest.setShipperServiceFee(shipper.getServiceFee());
        accountRequest.setOperator(SYSTEM_OPERATOR);
        Result<BigDecimal> accountResult = accountProtocol.settleShipperMoneyToCompanyAccount(accountRequest);
        if (accountResult == null || !accountResult.isSuccess()) {
            String message = accountResult == null ? "账户服务无响应" : accountResult.getMessage();
            log.warn("货主清算失败：账户侧扣划未成功, settlementNo={}, orderId={}, message={}",
                    shipper.getSettlementNo(), shipper.getOrderId(), message);
            return Result.fail("资金清算失败：" + message);
        }
        BigDecimal actualFrozenAmount = accountResult.getData() == null ? frozenAmount : accountResult.getData();

        // 6) 回写清算单：已结算 + 实付（实际扣划冻结额 + 货主服务费，均已结算至平台对公账户）
        BigDecimal serviceFee = shipper.getServiceFee() == null ? BigDecimal.ZERO : shipper.getServiceFee();
        BigDecimal paidAmount = scale(actualFrozenAmount.add(serviceFee));
        BigDecimal refundAmount = scale(frozenAmount.subtract(actualFrozenAmount));
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = scale(BigDecimal.ZERO);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean settled = settlementShipperService.lambdaUpdate()
                .set(SettlementShipper::getStatus, SettlementConstants.STATUS_SETTLED)
                .set(SettlementShipper::getStatusDesc, SettlementConstants.STATUS_DESC_SETTLED)
                .set(SettlementShipper::getPaidAmount, paidAmount)
                .set(SettlementShipper::getRefundAmount, refundAmount)
                .set(SettlementShipper::getSettleTime, now)
                .set(SettlementShipper::getUpdateTime, now)
                .set(SettlementShipper::getUpdateName, SYSTEM_OPERATOR)
                .eq(SettlementShipper::getId, shipper.getId())
                .in(SettlementShipper::getStatus,
                        SettlementConstants.STATUS_PENDING, SettlementConstants.STATUS_SETTLING)
                .update();
        if (!settled) {
            // 账户侧已按冻结流水号幂等，不会重复扣款；此处只需确认是否已被并发请求置为已结算
            SettlementShipper latest = settlementShipperService.getById(shipper.getId());
            if (latest != null && latest.getStatus() != null && latest.getStatus() == SettlementConstants.STATUS_SETTLED) {
                log.info("货主清算单已由并发请求置为已结算，工单按成功收口: settlementNo={}", shipper.getSettlementNo());
                return Result.success();
            }
            log.error("货主清算单回写失败，需人工核对资金与单据: settlementNo={}, orderId={}",
                    shipper.getSettlementNo(), shipper.getOrderId());
            return Result.fail("货主清算单回写失败，请稍后重试");
        }
        log.info("货主清算完成: settlementNo={}, orderId={}, shipperUserId={}, frozenAmount={}, 实扣冻结额={}, 服务费={}, paidAmount={}, refundAmount={}",
                shipper.getSettlementNo(), shipper.getOrderId(), shipper.getShipperUserId(),
                frozenAmount, actualFrozenAmount, serviceFee, paidAmount, refundAmount);
        return Result.success();
    }

    /**
     * 清算单状态 CAS（待结算 -> 结算中）
     */
    private boolean casShipperStatus(Long id, byte fromStatus, byte toStatus) {
        LocalDateTime now = LocalDateTime.now();
        return settlementShipperService.lambdaUpdate()
                .set(SettlementShipper::getStatus, toStatus)
                .set(SettlementShipper::getStatusDesc, SettlementConstants.STATUS_DESC_SETTLING)
                .set(SettlementShipper::getUpdateTime, now)
                .set(SettlementShipper::getUpdateName, SYSTEM_OPERATOR)
                .eq(SettlementShipper::getId, id)
                .eq(SettlementShipper::getStatus, fromStatus)
                .update();
    }

    // ==================================================================================
    // 查询与小工具
    // ==================================================================================

    private SettlementShipper queryShipper(String settlementNo) {
        return settlementShipperService.lambdaQuery()
                .eq(SettlementShipper::getSettlementNo, settlementNo)
                .eq(SettlementShipper::getDeleteFlag, SettlementConstants.DELETE_FLAG_NO)
                .last("limit 1")
                .one();
    }

    private SettlementShipperWorkOrder queryWorkOrder(String settlementNo) {
        return settlementShipperWorkOrderService.lambdaQuery()
                .eq(SettlementShipperWorkOrder::getSettlementNo, settlementNo)
                .eq(SettlementShipperWorkOrder::getDeleteFlag, SettlementConstants.DELETE_FLAG_NO)
                .last("limit 1")
                .one();
    }

    private boolean isWorkOrderSuccess(SettlementShipperWorkOrder workOrder) {
        return workOrder.getStatus() != null
                && workOrder.getStatus() == SettlementConstants.WORK_ORDER_STATUS_SUCCESS;
    }

    private boolean isRetryExhausted(SettlementShipperWorkOrder workOrder) {
        if (workOrder.getStatus() == null || workOrder.getStatus() != SettlementConstants.WORK_ORDER_STATUS_FAIL) {
            return false;
        }
        int retryCount = workOrder.getRetryCount() == null ? 0 : workOrder.getRetryCount();
        int maxRetryCount = workOrder.getMaxRetryCount() == null
                ? SettlementConstants.WORK_ORDER_MAX_RETRY_COUNT : workOrder.getMaxRetryCount();
        return retryCount >= maxRetryCount;
    }

    /**
     * 幂等键：SHIPPER_CLEARING:{orderId}，并按 uk_idempotent_key 字段长度截断
     */
    private String buildIdempotentKey(String orderId) {
        String key = SettlementConstants.SHIPPER_CLEARING_IDEMPOTENT_KEY_PREFIX + orderId;
        return key.length() > MAX_IDEMPOTENT_KEY_LENGTH ? key.substring(0, MAX_IDEMPOTENT_KEY_LENGTH) : key;
    }

    private String validateParam(String settlementNo, String orderId) {
        if (isBlank(settlementNo)) {
            return "清算单号为空";
        }
        if (isBlank(orderId)) {
            return "运单号为空";
        }
        if (settlementNo.trim().length() > 32) {
            return "清算单号长度非法";
        }
        if (orderId.trim().length() > 64) {
            return "运单号长度非法";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount == null ? null : amount.setScale(SettlementConstants.AMOUNT_SCALE, RoundingMode.HALF_UP);
    }
}
