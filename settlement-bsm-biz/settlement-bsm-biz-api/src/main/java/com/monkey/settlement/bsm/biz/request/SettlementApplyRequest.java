package com.monkey.settlement.bsm.biz.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 结算申请建单请求
 * <p>
 * 安全约定：金额、单号一律由清算服务端生成/计算，调用方仅提供运单快照信息；
 * 服务费率可不传，缺省取清算服务端配置，且服务端只接受 [0,1) 区间的费率。
 *
 * @author gkk
 * @since 2026-09-12
 */
@Data
public class SettlementApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 运单号（必填，幂等键；对应 tf_b_order.order_id）
     */
    private String orderId;

    /**
     * 货主用户ID（必填，来源登录态）
     */
    private String shipperUserId;

    /**
     * 货主用户名称（冗余快照）
     */
    private String shipperUserName;

    /**
     * 货主名称/企业名称（冗余快照）
     */
    private String shipperName;

    /**
     * 货主手机号（冗余快照）
     */
    private String shipperMobile;

    /**
     * 承运方用户ID（必填，运单成交后必须存在）
     */
    private String carrierUserId;

    /**
     * 承运方用户名称（冗余快照）
     */
    private String carrierUserName;

    /**
     * 承运方名称/车队名称（冗余快照）
     */
    private String carrierName;

    /**
     * 承运方手机号（冗余快照）
     */
    private String carrierMobile;

    /**
     * 订单运费金额（必填，>0，取运单快照，作为两侧清算的金额基准）
     */
    private BigDecimal transportMoney;

    /**
     * 已托管冻结金额（可选，为空时按运费兜底；用于校验托管是否足额）
     */
    private BigDecimal frozenAmount;

    /**
     * 托管冻结流水号（可选，账户侧冻结明细流水号，便于结算时追溯扣划来源）
     */
    private String frozenNo;

    /**
     * 承运方承担的服务费率（可选，如 0.0500 表示 5%；为空取服务端配置）
     */
    private BigDecimal carrierServiceFeeRate;

    /**
     * 货主承担的服务费率（可选；为空取服务端配置，默认 0）
     */
    private BigDecimal shipperServiceFeeRate;

    /**
     * 操作人（审计用，通常为登录货主ID）
     */
    private String operator;

    /**
     * 备注（写入两张清算单）
     */
    private String remark;
}
