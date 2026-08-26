package com.monkey.user.bsm.biz.protocol;

import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.PasswordUtil;
import com.monkey.ams.common.utils.SnowflakeIdWorker;
import com.monkey.user.bsm.api.dto.User;
import com.monkey.user.bsm.api.protocol.UserProtocol;
import com.monkey.user.bsm.biz.entity.UserEntity;
import com.monkey.user.bsm.biz.service.inf.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Objects;

/**
 * 用户服务实现（Dubbo 提供者）
 *
 * @author gkk
 */
@Slf4j
@DubboService(version = "1.0.0", group = "dev", timeout = 5000)
public class UserProtocolImpl implements UserProtocol {

    private static final String REGEX_MOBILE = "^1[3-9]\\d{9}$";

    @DubboReference(version = "1.0.0", group = "dev", timeout = 5000)
    private AccountProtocol accountProtocol;

    @Autowired
    private UserService userService;

    @Autowired
    private SnowflakeIdWorker idService;

    @Override
    public Result<User> register(User user) {
        // 1. 参数校验
        if (user == null || !StringUtils.hasText(user.getMobile())) {
            return Result.fail("手机号不能为空");
        }
        if (!user.getMobile().matches(REGEX_MOBILE)) {
            return Result.fail("手机号格式不正确");
        }
        if (!StringUtils.hasText(user.getPassword())
                || user.getPassword().length() < 6 || user.getPassword().length() > 20) {
            return Result.fail("密码长度需为6~20位");
        }

        // 2. 校验手机号是否已注册
        long count = userService.lambdaQuery()
                .eq(UserEntity::getMobile, user.getMobile())
                .count();
        if (count > 0) {
            return Result.fail("该手机号已注册");
        }

        // 3. 生成用户ID、加密密码、入库
        UserEntity entity = new UserEntity();
        entity.setUserId(Long.parseLong(idService.nextId()));
        entity.setUserName(StringUtils.hasText(user.getUserName())
                ? user.getUserName()
                : "用户" + user.getMobile().substring(7));
        entity.setMobile(user.getMobile());
        entity.setPassword(user.getPassword());
        entity.setCreateTime(new Date());
        userService.save(entity);
        log.info("用户注册成功: mobile={}", user.getMobile());

        return Result.success("注册成功", toUser(entity));
    }

    @Override
    public Result<User> login(User user) {
        // 1. 参数校验
        if (user == null || !StringUtils.hasText(user.getMobile())) {
            return Result.fail("手机号不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            return Result.fail("密码不能为空");
        }

        // 2. 按手机号查询
        UserEntity entity = userService.lambdaQuery()
                .eq(UserEntity::getMobile, user.getMobile())
                .one();
        if (entity == null) {
            return Result.fail("该手机号未注册");
        }

        // 3. 校验密码
        if (!Objects.equals(user.getPassword(), entity.getPassword())) {
            return Result.fail("密码错误");
        }
        log.info("用户登录成功: mobile={}", user.getMobile());

        return Result.success("登录成功", toUser(entity));
    }

    /**
     * 实体转DTO（剔除密码）
     */
    private User toUser(UserEntity entity) {
        User user = new User();
        user.setUserId(entity.getUserId());
        user.setUserName(entity.getUserName());
        user.setMobile(entity.getMobile());
        return user;
    }
}
