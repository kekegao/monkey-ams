package com.monkey.account.bsm.biz.protocol;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.account.bsm.biz.entity.Account;
import com.monkey.account.bsm.biz.service.inf.AccountService;
import com.monkey.ams.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Slf4j
@DubboService
public class AccountProtocolImpl implements AccountProtocol {

    @Autowired
    private AccountService accountService;

    @Override
    public Result openAccount(JSONObject jsonObject) {

        Account account = new Account();
        account.setUserId(jsonObject.getString("userId"));
        account.setUserName(jsonObject.getString("userName"));
        account.setMobile(jsonObject.getString("mobile"));
        account.setRealName(jsonObject.getString("realName"));
        account.setCreateName(jsonObject.getString("realName"));
        account.setCreateTime(new Date());
        accountService.save(account);

        return Result.success();
    }
}
