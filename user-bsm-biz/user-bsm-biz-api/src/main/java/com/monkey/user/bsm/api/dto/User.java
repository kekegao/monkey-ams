package com.monkey.user.bsm.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String userName;

    /**
     * 用户类型 1货主 2司机
     */
    private Integer userType;

    private String userTypeDesc;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 密码
     */
    private String password;
}
