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

    // ==================== 货主清算 MQ 工单（tf_b_settlement_shipper_work_order） ====================

    /**
     * 货主清算工单号前缀（HJSG + 23位流水）
     */
    public static final String SHIPPER_WORK_ORDER_NO_PREFIX = "HJSG";

    /**
     * 货主清算工单幂等键前缀（SHIPPER_CLEARING:{orderId}），对应 uk_idempotent_key
     */
    public static final String SHIPPER_CLEARING_IDEMPOTENT_KEY_PREFIX = "SHIPPER_CLEARING:";

    /**
     * 货主清算消息类型（落工单 message_type）
     */
    public static final String MESSAGE_TYPE_SHIPPER_CLEARING = "SETTLEMENT_SHIPPER_CLEARING";

    /**
     * 清算消息默认延迟投递秒数（30S）
     */
    public static final int WORK_ORDER_DELAY_SECONDS = 30;

    /**
     * 工单默认最大重试次数（超出后不再自动重试，转人工）
     */
    public static final int WORK_ORDER_MAX_RETRY_COUNT = 5;

    /**
     * 工单重试间隔秒数（下次重试时间 = 当前时间 + 该间隔）
     */
    public static final int WORK_ORDER_RETRY_INTERVAL_SECONDS = 30;

    /**
     * 工单状态：1 待投递
     */
    public static final byte WORK_ORDER_STATUS_WAIT_SEND = 1;

    /**
     * 工单状态：2 已投递
     */
    public static final byte WORK_ORDER_STATUS_SENT = 2;

    /**
     * 工单状态：3 处理中
     */
    public static final byte WORK_ORDER_STATUS_PROCESSING = 3;

    /**
     * 工单状态：4 处理成功
     */
    public static final byte WORK_ORDER_STATUS_SUCCESS = 4;

    /**
     * 工单状态：5 处理失败（可重试）
     */
    public static final byte WORK_ORDER_STATUS_FAIL = 5;

    /**
     * 工单状态：6 已作废
     */
    public static final byte WORK_ORDER_STATUS_CANCELLED = 6;

    /**
     * 工单状态描述：待投递
     */
    public static final String WORK_ORDER_DESC_WAIT_SEND = "待投递";

    /**
     * 工单状态描述：已投递
     */
    public static final String WORK_ORDER_DESC_SENT = "已投递";

    /**
     * 工单状态描述：处理中
     */
    public static final String WORK_ORDER_DESC_PROCESSING = "处理中";

    /**
     * 工单状态描述：处理成功
     */
    public static final String WORK_ORDER_DESC_SUCCESS = "处理成功";

    /**
     * 工单状态描述：处理失败
     */
    public static final String WORK_ORDER_DESC_FAIL = "处理失败";
}
