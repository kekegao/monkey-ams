package com.monkey.user.bsm.api.dto;

import lombok.Data;

@Data
public class User {

    private Long userId;

    private String userName;

    private String mobile;

    private String password;
}
