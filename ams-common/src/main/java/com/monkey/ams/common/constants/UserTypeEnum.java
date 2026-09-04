package com.monkey.ams.common.constants;

/**
 * 用户类型枚举
 */
public enum UserTypeEnum {

    /** 货主 */
    SHIPPER(1, "货主"),
    /** 司机 */
    DRIVER(2, "司机"),
    ;

    private final int value;
    private final String name;

    UserTypeEnum(int value, String name) {
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
    public static UserTypeEnum getByValue(int value) {
        for (UserTypeEnum type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}
