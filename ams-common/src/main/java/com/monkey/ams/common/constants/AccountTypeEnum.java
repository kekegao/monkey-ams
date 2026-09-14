package com.monkey.ams.common.constants;

/**
 * 智运宝账户类型：区分普通用户账户与平台公司对公账户
 * <p>
 * 平台公司对公账户由配置 account.platform.company-user-id 指定，
 * 货主清算的收款方与承运方对账划账的付款方均为此账户。
 *
 * @author gkk
 * @since 2026-09-14
 */
public enum AccountTypeEnum {

    /** 1用户账户（货主/承运方） */
    USER(1, "用户账户"),
    /** 2平台公司对公账户 */
    PLATFORM_COMPANY(2, "平台公司对公账户"),
    ;

    private final int value;
    private final String name;

    AccountTypeEnum(int value, String name) {
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

    public static AccountTypeEnum getByValue(int value) {
        for (AccountTypeEnum item : values()) {
            if (item.value == value) {
                return item;
            }
        }
        return null;
    }
}
