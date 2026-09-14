package com.monkey.ams.common.constants;

/**
 * 账户收入支出流水 - 记账方向
 * <p>
 * 1收入 / 2支出 影响账户余额（balance）；
 * 3冻结 / 4解冻 只在「可用余额」与「冻结金额」之间转换，不影响余额。
 *
 * @author gkk
 * @since 2026-09-14
 */
public enum IncomeExpenseDirectionEnum {

    /** 1收入：账户余额增加 */
    INCOME(1, "收入"),
    /** 2支出：账户余额减少 */
    EXPENSE(2, "支出"),
    /** 3冻结：可用余额转冻结金额 */
    FROZEN(3, "冻结"),
    /** 4解冻：冻结金额转回可用余额 */
    UNFROZEN(4, "解冻"),
    ;

    private final int value;
    private final String name;

    IncomeExpenseDirectionEnum(int value, String name) {
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
     * 实体字段为 TINYINT，此处直接返回 byte 便于落库
     */
    public byte byteValue() {
        return (byte) value;
    }

    public static IncomeExpenseDirectionEnum getByValue(int value) {
        for (IncomeExpenseDirectionEnum item : values()) {
            if (item.value == value) {
                return item;
            }
        }
        return null;
    }
}
