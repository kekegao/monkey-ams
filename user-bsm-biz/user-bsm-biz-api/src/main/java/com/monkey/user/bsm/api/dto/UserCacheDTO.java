package com.monkey.user.bsm.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserCacheDTO implements Serializable {

    private String userId;

    private String mobile;

    private String nickname;

    private String avatar;

    private Integer status;

    private Integer realNameStatus;

    private Integer driverStatus;
}
