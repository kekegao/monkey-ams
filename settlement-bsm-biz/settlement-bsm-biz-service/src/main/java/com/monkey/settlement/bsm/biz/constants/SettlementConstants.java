package com.monkey.settlement.bsm.biz.constants;

/**
 * 清算（结算单）模块常量
 *
 * @author gkk
 * @since 2026-09-12
 */
public final class SettlementConstants {

    private SettlementConstants() {
    }

    /**
     * 货主清算单单号前缀（HJS + 23位流水）
     */
    public static final String SHIPPER_SETTLEMENT_NO_PREFIX = "HJS";

    /**
     * 承运方清算单单号前缀（CJS + 23位流水）
     */
    public static final String CARRIER_SETTLEMENT_NO_PREFIX = "CJS";

    /**
     * 结算状态：1 待结算
     */
    public static final byte STATUS_PENDING = 1;

    /**
     * 结算状态：2 结算中
     */
    public static final byte STATUS_SETTLING = 2;

    /**
     * 结算状态：3 已结算
     */
    public static final byte STATUS_SETTLED = 3;

    /**
     * 结算状态：4 已作废
     */
    public static final byte STATUS_CANCELLED = 4;

    /**
     * 状态描述：待结算
     */
    public static final String STATUS_DESC_PENDING = "待结算";

    /**
     * 状态描述：结算中
     */
    public static final String STATUS_DESC_SETTLING = "结算中";

    /**
     * 状态描述：已结算
     */
    public static final String STATUS_DESC_SETTLED = "已结算";

    /**
     * 状态描述：已作废
     */
    public static final String STATUS_DESC_CANCELLED = "已作废";

    /**
     * 开票状态：0 未开票
     */
    public static final byte INVOICE_STATUS_NONE = 0;

    /**
     * 删除标记：0 正常
     */
    public static final byte DELETE_FLAG_NO = 0;

    /**
     * 金额精度（分）
     */
    public static final int AMOUNT_SCALE = 2;

    /**
     * 单笔运费金额上限（1 亿），用于拦截异常入参，避免脏数据放大
     */
    public static final java.math.BigDecimal MAX_TRANSPORT_MONEY = new java.math.BigDecimal("100000000");
}
