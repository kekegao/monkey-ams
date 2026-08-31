package com.monkey.user.bsm.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginSession implements Serializable {

    private String userId;

    private String sessionId;

    private String deviceId;

    private Long loginTime;
}
