package com.monkey.ams.common.constants;

/**
 * 订单状态枚举
 * <p>
 * 状态机：1发布 -> 2摘单 -> 3成交 -> 4发货 -> 5确认收货 -> 6回单确认 -> 7结算申请 -> 8结算 -> 9对账 -> 10发票
 */
public enum OrderStatusEnum {

    /** 1 发布（货源大厅可被摘单） */
    PUBLISH(1, "发布"),
    /** 2 摘单（承运方已接单，待货主成交） */
    ACCEPT(2, "摘单"),
    /** 3 成交（货主确认成交，达成正式合作） */
    DEAL(3, "成交"),
    /** 4 发货（承运方确认发货，启动运输） */
    SHIP(4, "发货"),
    /** 5 确认收货（货主确认货物到达） */
    CONFIRM_RECEIPT(5, "确认收货"),
    /** 6 回单确认（回单签收核对） */
    RECEIPT_CONFIRM(6, "回单确认"),
    /** 7 结算申请（发起运费结算） */
    SETTLEMENT_APPLY(7, "结算申请"),
    /** 8 结算（结算完成） */
    SETTLEMENT(8, "结算"),
    /** 9 对账（账务核对） */
    RECONCILIATION(9, "对账"),
    /** 10 发票（开票完成，流程终态） */
    INVOICE(10, "发票"),
    ;

    private final int value;
    private final String name;

    OrderStatusEnum(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据 value 获取枚举
     */
    public static OrderStatusEnum getByValue(int value) {
        for (OrderStatusEnum status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }
}
