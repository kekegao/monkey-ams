package com.monkey.ams.common.auth.context;

import com.monkey.ams.common.auth.model.LoginSession;

public final class UserContext {

    private static final ThreadLocal<LoginSession> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginSession session) {
        HOLDER.set(session);
    }

    public static LoginSession get() {
        return HOLDER.get();
    }

    public static String getUserId() {

        LoginSession session = HOLDER.get();

        return session == null
                ? null
                : session.getUserId();
    }

    public static String getSessionId() {

        LoginSession session = HOLDER.get();

        return session == null
                ? null
                : session.getSessionId();
    }

    public static String getDeviceId() {

        LoginSession session = HOLDER.get();

        return session == null
                ? null
                : session.getDeviceId();
    }

    public static boolean isLogin() {
        return HOLDER.get() != null;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
