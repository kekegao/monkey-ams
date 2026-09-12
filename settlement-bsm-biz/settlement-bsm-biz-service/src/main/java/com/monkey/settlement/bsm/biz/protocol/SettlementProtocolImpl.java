package com.monkey.settlement.bsm.biz.protocol;

import com.monkey.account.bsm.biz.api.FrozenDetailProtocol;
import com.monkey.account.bsm.biz.dto.FrozenDetailDto;
import com.monkey.ams.common.response.Result;
import com.monkey.common.lock.annotation.DistributedLock;
import com.monkey.settlement.bsm.biz.api.SettlementProtocol;
import com.monkey.settlement.bsm.biz.constants.SettlementConstants;
import com.monkey.settlement.bsm.biz.dto.SettlementApplyResultDto;
import com.monkey.settlement.bsm.biz.dto.SettlementCarrierDto;
import com.monkey.settlement.bsm.biz.dto.SettlementShipperDto;
import com.monkey.settlement.bsm.biz.request.SettlementApplyRequest;
import com.monkey.settlement.bsm.biz.service.inf.SettlementService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;

/**
 * 清算（结算单）服务协议实现
 * <p>
 * 分层职责：本类只做「入口防护 + 并发串行 + 结果包装」，业务与事务在 {@link SettlementService}，
 * 保证分布式锁位于事务切面之外（锁覆盖到事务提交之后），避免锁提前释放导致并发穿透。
 *
 * @author gkk
 * @since 2026-09-12
 */
@Slf4j
@DubboService
public class SettlementProtocolImpl implements SettlementProtocol {

    @Resource
    private SettlementService settlementService;

    /**
     * 账户冻结明细服务（account-bsm-biz-service）：用于反查运单托管中的运费冻结流水号
     */
    @DubboReference
    private FrozenDetailProtocol frozenDetailProtocol;

    /**
     * 结算申请建单
     * <p>
     * 并发控制：按运单号加分布式锁，同一运单的重复/并发申请串行化；
     * 锁内再由服务层「幂等查询 + uk_order_id 唯一索引」双重防重，不会重复建单、不会重复结算。
     *
     * @param request 结算申请参数
     * @return 两张清算单的单号与金额信息
     */
    @DistributedLock(key = "'settlement:apply:' + #request.orderId", waitTime = 3, leaseTime = -1)
    @Override
    public Result<SettlementApplyResultDto> applySettlement(SettlementApplyRequest request) {
        String invalidMessage = validateApplyRequest(request);
        if (invalidMessage != null) {
            log.warn("结算申请建单参数校验失败: {}", invalidMessage);
            return Result.fail(invalidMessage);
        }
        // 事务外补全托管冻结信息（远程查询置于事务之外，避免长期占用数据库连接）
        supplementFrozenInfo(request);
        try {
            SettlementApplyResultDto result = settlementService.applySettlement(request);
            log.info("结算申请建单完成: orderId={}, shipperNo={}, carrierNo={}, existed={}",
                    request.getOrderId(), result.getShipperSettlementNo(),
                    result.getCarrierSettlementNo(), result.getExisted());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            // 业务校验失败（费率越界、金额异常等）：事务已回滚，不产生脏单
            log.warn("结算申请建单业务校验失败: orderId={}, message={}", request.getOrderId(), e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 兜底：写库失败/唯一索引冲突等，调用方保持原状态并重试，重试将命中幂等查询
            log.error("结算申请建单失败: orderId={}", request.getOrderId(), e);
            return Result.fail("结算单生成失败，请稍后重试");
        }
    }

    @Override
    public Result<SettlementShipperDto> queryShipperSettlement(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return Result.fail("运单号不能为空");
        }
        try {
            return Result.success(settlementService.queryShipperSettlement(orderId.trim()));
        } catch (Exception e) {
            log.error("查询货主清算单失败: orderId={}", orderId, e);
            return Result.fail("查询货主清算单失败，请稍后重试");
        }
    }

    @Override
    public Result<SettlementCarrierDto> queryCarrierSettlement(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return Result.fail("运单号不能为空");
        }
        try {
            return Result.success(settlementService.queryCarrierSettlement(orderId.trim()));
        } catch (Exception e) {
            log.error("查询承运方清算单失败: orderId={}", orderId, e);
            return Result.fail("查询承运方清算单失败，请稍后重试");
        }
    }

    /**
     * 补全运单托管冻结信息（在数据库事务之外执行，避免事务内做远程调用）
     * <p>
     * 背景：冻结流水号由账户侧在「发布货源冻结运费」时生成，仅落库于 tf_b_frozen_detail
     * （订单表未持久化），因此清算服务在此按「货主 + 运单号」反查账户侧处于托管中的运费冻结明细
     * （biz_type=1 运费托管、status=1 冻结中），回填货主清算单的 frozen_no，
     * 使结算(8) 阶段可按流水号精确扣划与追溯。
     * <p>
     * 容错（高可用）：账户服务超时/异常/查无记录时不阻断建单（降级为无流水号建单）并告警，
     * 由结算(8) 阶段再次以账户侧数据为准做资金校验与扣划，避免账户域抖动导致结算流程整体卡死。
     *
     * @param request 结算申请参数（就地补全 frozenNo / frozenAmount）
     */
    private void supplementFrozenInfo(SettlementApplyRequest request) {
        // 调用方已明确指定托管流水号（如平台指定、数据修复场景），不再反查
        if (!isBlank(request.getFrozenNo())) {
            return;
        }
        try {
            Result<FrozenDetailDto> frozenResult = frozenDetailProtocol.selectTransportFrozenDetail(
                    request.getShipperUserId(), request.getOrderId());
            if (frozenResult == null || !frozenResult.isSuccess() || frozenResult.getData() == null) {
                log.warn("未查询到运单处于托管中的运费冻结记录，清算单暂缺冻结流水号: orderId={}, message={}",
                        request.getOrderId(), frozenResult == null ? null : frozenResult.getMessage());
                return;
            }
            FrozenDetailDto frozenDetail = frozenResult.getData();
            request.setFrozenNo(frozenDetail.getFrozenNo());
            // 托管金额以账户侧实际冻结额为准（资金唯一真相源），与调用方传入值不一致时告警
            BigDecimal accountFrozenAmount = frozenDetail.getAmount();
            if (accountFrozenAmount != null && accountFrozenAmount.signum() > 0
                    && (request.getFrozenAmount() == null
                    || request.getFrozenAmount().compareTo(accountFrozenAmount) != 0)) {
                log.warn("运单已托管金额与申请金额不一致，以账户实际冻结额为准: orderId={}, requestAmount={}, accountAmount={}",
                        request.getOrderId(), request.getFrozenAmount(), accountFrozenAmount);
                request.setFrozenAmount(accountFrozenAmount);
            }
            log.info("已补全运单托管冻结信息: orderId={}, frozenNo={}, frozenAmount={}",
                    request.getOrderId(), frozenDetail.getFrozenNo(), frozenDetail.getAmount());
        } catch (Exception e) {
            // 降级：账户域异常不影响建单，结算阶段会再次校验资金
            log.warn("反查运单托管冻结记录异常，降级生成清算单: orderId={}", request.getOrderId(), e);
        }
    }

    /**
     * 入口参数校验：只校验必要字段与非空金额，金额结果一律由服务端计算
     *
     * @return 校验不通过时返回提示信息，通过返回 null
     */
    private String validateApplyRequest(SettlementApplyRequest request) {
        if (request == null) {
            return "结算申请参数不能为空";
        }
        if (isBlank(request.getOrderId())) {
            return "运单号不能为空";
        }
        if (request.getOrderId().length() > 64) {
            return "运单号长度非法";
        }
        if (isBlank(request.getShipperUserId())) {
            return "货主信息缺失，无法生成清算单";
        }
        if (isBlank(request.getCarrierUserId())) {
            return "承运方信息缺失，无法生成清算单";
        }
        if (request.getTransportMoney() == null || request.getTransportMoney().signum() <= 0) {
            return "运单运费金额异常，无法生成清算单";
        }
        if (request.getTransportMoney().compareTo(SettlementConstants.MAX_TRANSPORT_MONEY) > 0) {
            return "运单运费金额超出合理范围，无法生成清算单";
        }
        if (request.getFrozenAmount() != null && request.getFrozenAmount().signum() < 0) {
            return "已托管冻结金额非法";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
