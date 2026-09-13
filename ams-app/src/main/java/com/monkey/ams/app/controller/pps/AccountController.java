package com.monkey.ams.app.controller.pps;

import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.account.bsm.biz.api.AccountRechargeProtocol;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.request.RechargeAccountRequest;
import com.monkey.ams.app.controller.BaseController;
import com.monkey.ams.common.response.Result;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智运宝账户：充值 / 余额查询
 */
@RestController
@RequestMapping("/account")
public class AccountController extends BaseController {

    @DubboReference
    private AccountRechargeProtocol accountRechargeProtocol;

    @DubboReference
    private AccountProtocol accountProtocol;

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

    /**
     * 账户余额查询（当前登录用户）
     * 完整调用链：
     * account.vue -> ams-app(AccountController) -> account-bsm-biz-service(AccountProtocolImpl)
     *   -> AccountServiceImpl -> AccountMapper(selectAccount) -> MyBatis -> tf_b_account 表
     *
     * POST /account/balance
     */
    @PostMapping("/balance")
    public Result<AccountDto> balance() {
        return accountProtocol.selectAccount(getUserId());
    }

}
