package com.monkey.account.bsm.biz.api;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.request.FrozenMoneyAccountRequest;
import com.monkey.account.bsm.biz.request.UnFrozenMoneyAccountRequest;
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
     * @param request
     * @return
     */
    Result frozenTransportMoneyAccount(FrozenMoneyAccountRequest request);


    /**
     * 释放运费
     *
     * @param request
     * @return
     */
    Result unfrozenTransportMoneyAccount(UnFrozenMoneyAccountRequest request);


    /**
     * 冻结承运方保证金
     *
     * @param request
     * @return
     */
    Result frozenCarrierMoneyAccount(FrozenMoneyAccountRequest request);

    /**
     * 释放承运方保证金
     *
     * @param request
     * @return
     */
    Result unFrozenCarrierMoneyAccount(UnFrozenMoneyAccountRequest request);

    /**
     * 查询智运宝账户
     *
     * @param userId
     * @return
     */
    Result<AccountDto> selectAccount(String userId);

}
