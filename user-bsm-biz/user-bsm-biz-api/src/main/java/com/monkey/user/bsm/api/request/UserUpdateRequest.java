package com.monkey.user.bsm.api.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateRequest  implements Serializable {

    private String userId;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String mobile;
}
