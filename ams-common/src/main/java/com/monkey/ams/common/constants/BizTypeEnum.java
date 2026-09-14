package com.monkey.ams.common.constants;

public enum BizTypeEnum {


    /** 1运费托管 */
    TRANSPORT_MONEY(1, "运费托管"),
    /** 2发货保证金冻结 */
    SHIP_MONEY(2, "发货保证金冻结"),
    /** 3提现申请冻结 */
    WITHDRAW_MONEY(3, "提现申请冻结"),
    /** 4平台划账（平台公司对公账户划付承运方清算款，仅作划账流水与幂等锚点） */
    PLATFORM_TRANSFER(4, "平台划账"),
    ;

    private final int value;
    private final String name;

    BizTypeEnum(int value, String name) {
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
    public static BizTypeEnum getByValue(int value) {
        for (BizTypeEnum type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}
