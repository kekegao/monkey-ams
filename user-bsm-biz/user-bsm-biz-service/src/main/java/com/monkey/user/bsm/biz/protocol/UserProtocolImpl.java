package com.monkey.user.bsm.biz.protocol;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.ams.common.auth.AuthConstants;
import com.monkey.ams.common.auth.model.LoginSession;
import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.SnowflakeIdWorker;
import com.monkey.common.lock.annotation.DistributedLock;
import com.monkey.user.bsm.api.dto.*;
import com.monkey.user.bsm.api.protocol.UserProtocol;
import com.monkey.user.bsm.api.request.LoginRequest;
import com.monkey.user.bsm.api.request.UserUpdateRequest;
import com.monkey.user.bsm.biz.entity.UserEntity;
import com.monkey.user.bsm.biz.service.inf.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import static com.monkey.ams.common.auth.AuthConstants.LOGIN_USER_SUFFIX;

/**
 * 用户服务实现（Dubbo 提供者）
 *
 * @author gkk
 */
@Slf4j
@DubboService
public class UserProtocolImpl implements UserProtocol {

    private static final String REGEX_MOBILE = "^1[3-9]\\d{9}$";

    @DubboReference
    private AccountProtocol accountProtocol;

    @Autowired
    private UserService userService;

    @Autowired
    private SnowflakeIdWorker idService;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @DistributedLock(key = "'register:user:' + #user.mobile", waitTime = 3, leaseTime = -1)
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
        entity.setUserId(idService.nextId());
        entity.setUserName(idService.generateUserName());
        entity.setMobile(user.getMobile());
        entity.setRealName(user.getRealName());
        entity.setPassword(user.getPassword());
        entity.setUserType(user.getUserType());
        entity.setUserTypeDesc(user.getUserTypeDesc());
        entity.setCreateTime(new Date());
        entity.setCreateName(user.getRealName());
        userService.save(entity);
        log.info("用户注册成功: mobile={}", user.getMobile());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mobile", user.getMobile());
        jsonObject.put("userId", entity.getUserId());
        jsonObject.put("userName", entity.getUserName());

        accountProtocol.openAccount(jsonObject);

        return Result.success("注册成功", toUser(entity));
    }

    @Override
    public Result<LoginResponse> login(LoginRequest request) {
        // 1. 参数校验
        if (request == null || !StringUtils.hasText(request.getMobile())) {
            return Result.fail("手机号不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            return Result.fail("密码不能为空");
        }

        // 2. 按手机号查询
        UserEntity entity = userService.lambdaQuery()
                .eq(UserEntity::getMobile, request.getMobile())
                .one();
        if (entity == null) {
            return Result.fail("该手机号未注册");
        }

        // 3. 校验密码
        if (!Objects.equals(request.getPassword(), entity.getPassword())) {
            return Result.fail("密码错误");
        }
        log.info("用户登录成功: mobile={}", request.getMobile());


        // 创建 Token
        String token = UUID.randomUUID().toString().replace("-", "");

        String sessionId = UUID.randomUUID().toString().replace("-", "");

        LoginSession session = new LoginSession();
        session.setUserId(entity.getUserId());
        session.setUserName(entity.getUserName());
        session.setMobile(request.getMobile());
        session.setSessionId(sessionId);
        session.setDeviceId(request.getDeviceId());

        String tokenKey = AuthConstants.LOGIN_TOKEN_PREFIX + token;

        stringRedisTemplate.opsForValue().set(
                tokenKey,
                JSON.toJSONString(session),
                Duration.ofHours(2)
        );

        return Result.success("登录成功", LoginResponse.builder()
                .token(token)
                .userId(entity.getUserId())
                .expire(7200L)
                .build());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result updateUser(UserUpdateRequest request) {
        String userId = request.getUserId();
        UserEntity user = userService.lambdaQuery()
                .eq(UserEntity::getUserId, userId)
                .one();
        if (user == null) {
            return Result.fail();
        }
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setRealName(request.getRealName());
        entity.setMobile(request.getMobile());
        userService.updateById(entity);
        String key = LOGIN_USER_SUFFIX + userId;
        stringRedisTemplate.delete(key);
        return Result.success();
    }

    @Override
    public Result<UserInfoDTO> getUser(String userId) {

        String key = LOGIN_USER_SUFFIX + userId;

        String json = stringRedisTemplate.opsForValue().get(key);

        if (json != null) {

            UserInfoDTO userInfoDTO = JSON.parseObject(
                    json,
                    UserInfoDTO.class
            );
            return Result.success(userInfoDTO);
        }

        UserEntity user = userService.lambdaQuery()
                .eq(UserEntity::getUserId, userId)
                .one();

        if (user == null) {
            return Result.fail();
        }

        UserInfoDTO dto = convert(user);

        stringRedisTemplate.opsForValue().set(
                key,
                JSON.toJSONString(dto),
                Duration.ofMinutes(120)
        );

        return Result.success(dto);
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

    /**
     * 实体转DTO（剔除密码）
     */
    private UserInfoDTO convert(UserEntity entity) {
        UserInfoDTO user = new UserInfoDTO();
        user.setUserId(entity.getUserId());
        user.setUserName(entity.getUserName());
        user.setMobile(entity.getMobile());
        user.setUserType(entity.getUserType());
        user.setUserTypeDesc(entity.getUserTypeDesc());
        return user;
    }
}
