package com.monkey.user.bsm.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String userName;

    private String mobile;

    private String password;
}
