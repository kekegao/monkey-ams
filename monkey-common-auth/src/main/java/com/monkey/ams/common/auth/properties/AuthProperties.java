package com.monkey.ams.common.auth.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "monkey.auth")
public class AuthProperties {

    /**
     * 是否开启认证
     */
    private boolean enabled = true;

    /**
     * 是否开启 Web 认证
     */
    private boolean webEnabled = true;

    /**
     * 是否开启 Dubbo Consumer 用户身份透传
     */
    private boolean dubboConsumerEnabled = true;

    /**
     * 是否开启 Dubbo Provider 用户身份恢复
     */
    private boolean dubboProviderEnabled = true;

    /**
     * Redis Token Key 前缀
     */
    private String tokenPrefix = "monkey:login:token:";

    /**
     * Authorization Header
     */
    private String headerName = "Authorization";

    /**
     * Token 前缀
     */
    private String tokenPrefixValue = "Bearer ";

    /**
     * 不需要登录的接口
     */
    private List<String> excludePaths = new ArrayList<>();
}
