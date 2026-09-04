package com.monkey.account.bsm.biz.protocol;

import com.monkey.account.bsm.biz.api.AccountRechargeProtocol;
import com.monkey.account.bsm.biz.request.RechargeAccountRequest;
import com.monkey.account.bsm.biz.service.inf.AccountService;
import com.monkey.ams.common.response.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@Slf4j
@DubboService
public class AccountRechargeProtocolImpl implements AccountRechargeProtocol {

    @Resource
    private AccountService accountService;
    /**
     * 充值
     *
     * @param request
     * @return
     */
    @Override
    public Result recharge(RechargeAccountRequest request) {
        accountService.selectAccount(request.getUserId());
        return null;
    }
}
