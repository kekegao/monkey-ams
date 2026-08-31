package com.monkey.ams.common.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monkey.ams.common.auth.AuthConstants;
import com.monkey.ams.common.auth.model.LoginSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class AuthSessionService {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public LoginSession getSession(String token) {

        String key =
                AuthConstants.LOGIN_TOKEN_PREFIX + token;

        String value =
                redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        try {

            return objectMapper.readValue(
                    value,
                    LoginSession.class
            );

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

        String key =
                AuthConstants.LOGIN_TOKEN_PREFIX + token;

        redisTemplate.expire(
                key,
                timeout,
                unit
        );
    }
}
