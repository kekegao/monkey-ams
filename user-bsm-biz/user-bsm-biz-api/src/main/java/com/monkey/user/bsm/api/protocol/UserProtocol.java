package com.monkey.user.bsm.api.protocol;

import com.monkey.ams.common.response.Result;
import com.monkey.user.bsm.api.dto.*;
import com.monkey.user.bsm.api.request.LoginRequest;
import com.monkey.user.bsm.api.request.UserUpdateRequest;

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

    /**
     * 更改会员信息
     *
     * @param request
     * @return
     */
    Result updateUser(UserUpdateRequest request);

    /**
     * 获取会员信息
     *
     * @param userId
     * @return
     */
    Result<UserInfoDTO> getUser(String userId);
}
