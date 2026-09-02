package com.monkey.user.bsm.api.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginRequest implements Serializable {

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 密码
     */
    private String password;

    /**
     * 设备ID
     */
    private String deviceId;
}
