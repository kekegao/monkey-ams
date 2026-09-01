package com.monkey.ams.common.auth.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monkey.ams.common.auth.properties.AuthProperties;
import com.monkey.ams.common.auth.service.AuthSessionService;
import com.monkey.ams.common.auth.web.AuthFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.AntPathMatcher;

@AutoConfiguration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthSessionService authSessionService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {

        return new AuthSessionService(
                redisTemplate,
                objectMapper
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthFilter authFilter(
            AuthProperties properties,
            AuthSessionService sessionService) {

        return new AuthFilter(
                properties,
                sessionService
        );
    }

    @Bean
    public FilterRegistrationBean<AuthFilter>
    authFilterRegistration(
            AuthFilter authFilter,
            AuthProperties properties) {

        FilterRegistrationBean<AuthFilter> bean =
                new FilterRegistrationBean<>();

        bean.setFilter(authFilter);

        bean.addUrlPatterns("/*");

        bean.setOrder(0);

        bean.setEnabled(properties.isEnabled());

        return bean;
    }
}
