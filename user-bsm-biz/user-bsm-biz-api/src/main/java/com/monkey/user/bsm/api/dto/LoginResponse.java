package com.monkey.user.bsm.api.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class LoginResponse implements Serializable {

    /**
     * 登录Token
     */
    private String token;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * Token过期时间，单位秒
     */
    private Long expire;
}
