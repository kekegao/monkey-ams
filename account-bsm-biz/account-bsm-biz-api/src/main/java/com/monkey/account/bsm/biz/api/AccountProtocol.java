package com.monkey.account.bsm.biz.api;

import com.alibaba.fastjson.JSONObject;
import com.monkey.ams.common.response.Result;

public interface AccountProtocol {

    /**
     * 开户
     *
     * @param jsonObject
     * @return
     */
    Result openAccount(JSONObject jsonObject);

}
