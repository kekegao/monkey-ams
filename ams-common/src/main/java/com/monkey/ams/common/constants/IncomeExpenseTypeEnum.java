package com.monkey.ams.common.constants;

/**
 * 账户收入支出流水 - 收支科目（财务统计口径）
 * <p>
 * 与业务类型（{@link IncomeExpenseBizTypeEnum}）区分：业务类型描述「钱因何而动」，
 * 收支科目描述「在财务上算什么」，用于收入/支出汇总与平台服务费统计。
 *
 * @author gkk
 * @since 2026-09-14
 */
public enum IncomeExpenseTypeEnum {

    /** 0不适用：冻结/解冻只改变可用与冻结结构，不计收支科目 */
    NOT_APPLICABLE(0, "不适用"),
    /** 1运费收入：承运方收到的运费、平台收到的托管运费 */
    TRANSPORT_INCOME(1, "运费收入"),
    /** 2运费支出：货主付出的运费、平台划付承运方的运费 */
    TRANSPORT_EXPENSE(2, "运费支出"),
    /** 3平台服务费：平台收取的佣金/服务费 */
    PLATFORM_SERVICE_FEE(3, "平台服务费"),
    /** 4充值 */
    RECHARGE(4, "充值"),
    /** 5提现 */
    WITHDRAW(5, "提现"),
    /** 6保证金 */
    DEPOSIT(6, "保证金"),
    /** 7人工调账 */
    MANUAL_ADJUST(7, "人工调账"),
    /** 8其他 */
    OTHER(8, "其他"),
    ;

    private final int value;
    private final String name;

    IncomeExpenseTypeEnum(int value, String name) {
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

    public static IncomeExpenseTypeEnum getByValue(int value) {
        for (IncomeExpenseTypeEnum item : values()) {
            if (item.value == value) {
                return item;
            }
        }
        return null;
    }
}
