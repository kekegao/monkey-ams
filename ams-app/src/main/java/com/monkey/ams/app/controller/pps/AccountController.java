package com.monkey.ams.app.controller.pps;

import com.monkey.account.bsm.biz.api.AccountRechargeProtocol;
import com.monkey.account.bsm.biz.request.RechargeAccountRequest;
import com.monkey.ams.app.controller.BaseController;
import com.monkey.ams.common.response.Result;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账户充值
 */
@RestController
@RequestMapping("/account")
public class AccountController extends BaseController {

    @DubboReference
    private AccountRechargeProtocol accountRechargeProtocol;

    /**
     * 充值
     *
     * POST /account/recharge
     */
    @PostMapping("/recharge")
    public Result recharge(@RequestBody RechargeAccountRequest request){
        request.setUserId(getUserId());
        return accountRechargeProtocol.recharge(request);
    }

}
