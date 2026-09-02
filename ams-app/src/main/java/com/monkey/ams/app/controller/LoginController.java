package com.monkey.ams.app.controller;

import com.monkey.ams.common.response.Result;
import com.monkey.user.bsm.api.request.LoginRequest;
import com.monkey.user.bsm.api.dto.LoginResponse;
import com.monkey.user.bsm.api.dto.User;
import com.monkey.user.bsm.api.protocol.UserProtocol;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录注册接口（登录账号为手机号）
 *
 * @author gkk
 */
@RestController
@RequestMapping("/app")
public class LoginController {

    @DubboReference
    private UserProtocol userProtocol;

    /**
     * 注册
     * POST /login/register  body: {"mobile":"13800138000","password":"123456","userName":"张三"}
     */
    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        return userProtocol.register(user);
    }

    /**
     * 登录（手机号 + 密码）
     * POST /login/login  body: {"mobile":"13800138000","password":"123456"}
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return userProtocol.login(request);
    }

}
