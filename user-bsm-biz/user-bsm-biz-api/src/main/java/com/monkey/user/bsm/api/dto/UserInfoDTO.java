package com.monkey.user.bsm.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfoDTO implements Serializable {

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

    private String mobile;

    private String nickname;

    private String avatar;

    private Integer status;

    private Integer realNameStatus;

    private Integer driverStatus;
}
