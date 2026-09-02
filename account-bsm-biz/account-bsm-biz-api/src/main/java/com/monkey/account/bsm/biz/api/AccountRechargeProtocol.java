package com.monkey.account.bsm.biz.api;

import com.monkey.account.bsm.biz.request.RechargeAccountRequest;
import com.monkey.ams.common.response.Result;

public interface AccountRechargeProtocol {

    /**
     * 充值
     *
     * @param request
     * @return
     */
    Result recharge(RechargeAccountRequest request);
}
