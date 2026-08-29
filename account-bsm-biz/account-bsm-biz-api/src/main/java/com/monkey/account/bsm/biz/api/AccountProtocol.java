package com.monkey.account.bsm.biz.api;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.ams.common.response.Result;

import java.math.BigDecimal;

public interface AccountProtocol {

    /**
     * 开户
     *
     * @param jsonObject
     * @return
     */
    Result openAccount(JSONObject jsonObject);


    /**
     * 冻结运费
     *
     * @param userId
     * @param amount
     * @return
     */
    Result frozenTransportMoneyAccount(String userId, BigDecimal amount);


    /**
     * 释放运费
     *
     * @param userId
     * @param amount
     * @return
     */
    Result unfrozenTransportMoneyAccount(String userId, BigDecimal amount);

    /**
     * 查询智运宝账户
     *
     * @param userId
     * @return
     */
    Result<AccountDto> selectAccount(String userId);

}
