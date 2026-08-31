package com.monkey.ams.common.auth;

public final class AuthConstants {

    private AuthConstants() {
    }

    /**
     * HTTP Header
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Token 前缀
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Redis Token Key 前缀
     */
    public static final String LOGIN_TOKEN_PREFIX = "monkey:login:token:";

    /**
     * User Context Request Attribute
     */
    public static final String USER_CONTEXT_ATTRIBUTE = "MONKEY_USER_CONTEXT";

    /**
     * Dubbo RPC 用户ID
     */
    public static final String RPC_USER_ID = "monkey-user-id";

    /**
     * Dubbo RPC Session ID
     */
    public static final String RPC_SESSION_ID = "monkey-session-id";

    /**
     * Dubbo RPC Device ID
     */
    public static final String RPC_DEVICE_ID = "monkey-device-id";
}
