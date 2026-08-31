package com.monkey.ams.common.auth.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * Session ID
     */
    private String sessionId;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 登录时间
     */
    private Long loginTime;
}
