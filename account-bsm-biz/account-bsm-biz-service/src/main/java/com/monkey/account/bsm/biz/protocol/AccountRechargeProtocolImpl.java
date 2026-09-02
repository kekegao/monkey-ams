package com.monkey.account.bsm.biz.protocol;

import com.monkey.account.bsm.biz.api.AccountRechargeProtocol;
import com.monkey.account.bsm.biz.request.RechargeAccountRequest;
import com.monkey.ams.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@Slf4j
@DubboService
public class AccountRechargeProtocolImpl implements AccountRechargeProtocol {

    /**
     * 充值
     *
     * @param request
     * @return
     */
    @Override
    public Result recharge(RechargeAccountRequest request) {

        return null;
    }
}
