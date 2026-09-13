package com.monkey.ams.common.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monkey.ams.common.auth.AuthConstants;
import com.monkey.ams.common.auth.model.LoginSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class AuthSessionService {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public LoginSession getSession(String token) {

        String key = AuthConstants.LOGIN_TOKEN_PREFIX + token;

        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        try {

            LoginSession loginSession = objectMapper.readValue(
                    value,
                    LoginSession.class
            );

            //续期缓存
            refreshSession(token,120,TimeUnit.MINUTES);
            return  loginSession;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to deserialize login session",
                    e
            );
        }
    }

    public void deleteSession(String token) {

        String key =
                AuthConstants.LOGIN_TOKEN_PREFIX + token;

        redisTemplate.delete(key);
    }

    public void refreshSession(
            String token,
            long timeout,
            TimeUnit unit) {

        try{
            String key =
                    AuthConstants.LOGIN_TOKEN_PREFIX + token;

            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

            if (ttl != null && ttl > 0 && ttl <= 30 * 60) {

                redisTemplate.expire(
                        key,
                        timeout,
                        unit);

            }
        } catch (Exception e) {
            log.error("refreshSession error", e);
        }

    }
}
