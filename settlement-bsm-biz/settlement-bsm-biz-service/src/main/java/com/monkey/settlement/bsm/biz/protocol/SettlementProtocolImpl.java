package com.monkey.settlement.bsm.biz.protocol;

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
import org.apache.dubbo.config.annotation.DubboService;

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
