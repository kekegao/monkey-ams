package com.monkey.user.bsm.api.protocol;

import com.monkey.ams.common.response.Result;
import com.monkey.user.bsm.api.dto.LoginRequest;
import com.monkey.user.bsm.api.dto.LoginResponse;
import com.monkey.user.bsm.api.dto.User;

/**
 * 用户服务接口（Dubbo）
 *
 * @author gkk
 */
public interface UserProtocol {

    /**
     * 注册（登录账号为手机号）
     *
     * @param user 手机号、密码（必填），用户名（可选）
     * @return 注册成功返回用户信息（不含密码）
     */
    Result<User> register(User user);

    /**
     * 手机号 + 密码登录
     *
     * @param request 手机号、密码
     * @return 登录成功返回用户信息（不含密码）
     */
    Result<LoginResponse> login(LoginRequest request);
}
