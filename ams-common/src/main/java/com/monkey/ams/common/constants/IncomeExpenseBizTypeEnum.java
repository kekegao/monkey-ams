package com.monkey.ams.common.constants;

/**
 * 账户收入支出流水 - 业务类型（资金动作场景）
 *
 * @author gkk
 * @since 2026-09-14
 */
public enum IncomeExpenseBizTypeEnum {

    /** 1充值：外部资金进入账户 */
    RECHARGE(1, "充值"),
    /** 2提现：账户资金出账至银行卡 */
    WITHDRAW(2, "提现"),
    /** 3运费托管：货主发布货源冻结运费、取消/异常解冻 */
    TRANSPORT_MONEY(3, "运费托管"),
    /** 4发货保证金：承运方确认发货冻结保证金、回单确认解冻 */
    SHIP_MONEY(4, "发货保证金"),
    /** 5货主清算扣划：货主托管运费（含货主服务费）-> 平台公司对公账户 */
    SHIPPER_SETTLE(5, "货主清算扣划"),
    /** 6承运方清算划账：平台公司对公账户 -> 承运方账户 */
    CARRIER_SETTLE(6, "承运方清算划账"),
    /** 7人工调账：运营差错处理，须成对记账 */
    MANUAL_ADJUST(7, "人工调账"),
    ;

    private final int value;
    private final String name;

    IncomeExpenseBizTypeEnum(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public byte byteValue() {
        return (byte) value;
    }

    public static IncomeExpenseBizTypeEnum getByValue(int value) {
        for (IncomeExpenseBizTypeEnum item : values()) {
            if (item.value == value) {
                return item;
            }
        }
        return null;
    }
}
