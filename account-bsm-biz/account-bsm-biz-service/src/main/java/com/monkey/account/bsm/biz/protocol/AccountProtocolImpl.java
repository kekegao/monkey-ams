package com.monkey.account.bsm.biz.protocol;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.entity.Account;
import com.monkey.account.bsm.biz.service.inf.AccountService;
import com.monkey.ams.common.response.Result;
import com.monkey.common.lock.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@DubboService
public class AccountProtocolImpl implements AccountProtocol {

    @Autowired
    private AccountService accountService;

    @DistributedLock(key = "'open:account:' + #param['userId']", waitTime = 3, leaseTime = -1)
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

    @DistributedLock(key = "'frozen:transportMoney:account:' + #userId", waitTime = 3, leaseTime = -1)
    @Override
    public Result frozenTransportMoneyAccount(String userId, BigDecimal amount) {

        // 参数校验：运费必须大于0
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("运费金额必须大于0");
        }

        Result<AccountDto> result = accountService.selectAccount(userId);
        if(!result.isSuccess()) {
            return result;
        }
        AccountDto accountDto = result.getData();
        if(accountDto.getAvailableAmount() == null || accountDto.getAvailableAmount().compareTo(amount) < 0) {
            return Result.fail("可用余额不足");
        }

        // 冻结运费：冻结金额累加运费，可用余额扣减运费
        BigDecimal frozenAmount = accountDto.getFrozenAmount() == null
                ? BigDecimal.ZERO : accountDto.getFrozenAmount();
        BigDecimal newFrozenAmount = frozenAmount.add(amount);
        BigDecimal newAvailableAmount = accountDto.getAvailableAmount().subtract(amount);

        Account account = new Account();
        account.setId(accountDto.getId());
        account.setFrozenAmount(newFrozenAmount);
        account.setAvailableAmount(newAvailableAmount);
        account.setUpdateTime(new Date());
        account.setUpdateName(accountDto.getRealName());
        accountService.updateById(account);

        log.info("冻结运费成功: userId={}, amount={}, frozenAmount={}, availableAmount={}",
                userId, amount, newFrozenAmount, newAvailableAmount);

        return Result.success();
    }

    @DistributedLock(key = "'unfrozen:transportMoney:account:' + #userId", waitTime = 3, leaseTime = -1)
    @Override
    public Result unfrozenTransportMoneyAccount(String userId, BigDecimal amount) {

        // 参数校验：解冻运费必须大于0
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("解冻运费金额必须大于0");
        }

        Result<AccountDto> result = accountService.selectAccount(userId);
        if(!result.isSuccess()) {
            return result;
        }
        AccountDto accountDto = result.getData();

        // 校验冻结余额是否足够释放
        BigDecimal frozenAmount = accountDto.getFrozenAmount() == null
                ? BigDecimal.ZERO : accountDto.getFrozenAmount();
        if(frozenAmount.compareTo(amount) < 0) {
            return Result.fail("冻结金额不足，无法释放");
        }

        // 释放运费：冻结金额扣减运费，可用余额加回运费
        BigDecimal newFrozenAmount = frozenAmount.subtract(amount);
        BigDecimal newAvailableAmount = accountDto.getAvailableAmount().add(amount);

        Account account = new Account();
        account.setId(accountDto.getId());
        account.setFrozenAmount(newFrozenAmount);
        account.setAvailableAmount(newAvailableAmount);
        account.setUpdateTime(new Date());
        account.setUpdateName(accountDto.getRealName());
        accountService.updateById(account);

        log.info("释放运费成功: userId={}, amount={}, frozenAmount={}, availableAmount={}",
                userId, amount, newFrozenAmount, newAvailableAmount);

        return Result.success();
    }

    @Override
    public Result<AccountDto> selectAccount(String userId) {
        return accountService.selectAccount(userId);
    }
}
