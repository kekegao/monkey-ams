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

    /**
     * 用户类型 1货主 2司机
     */
    private Integer userType;

    /**
     * 用户类型描述（货主/司机）
     */
    private String userTypeDesc;
}
