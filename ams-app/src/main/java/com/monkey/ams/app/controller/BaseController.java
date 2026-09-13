package com.monkey.ams.app.controller;


import com.monkey.ams.common.auth.context.UserContext;
import com.monkey.ams.common.auth.model.LoginSession;

public abstract class BaseController {

    /**
     * 获取当前用户ID
     */
    protected String getUserId() {
        return UserContext.getUserId();
    }

    /**
     * 获取当前登录Session
     */
    protected LoginSession getSession() {
        return UserContext.get();
    }

    /**
     * 获取当前用户名
     */
    protected String getUserName() {
        return getSession().getUserName();
    }
}
