package com.monkey.account.bsm.biz.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.entity.Account;
import com.monkey.account.bsm.biz.mapper.AccountMapper;
import com.monkey.account.bsm.biz.service.inf.AccountService;
import com.monkey.ams.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * <p>
 * 账户表 服务实现类
 * </p>
 *
 * @author gkk
 * @since 2026-08-26
 */
@Slf4j
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    @Override
    public Result<AccountDto> selectAccount(String userId) {
        AccountDto accountDto = this.baseMapper.selectAccount(userId);
        if(accountDto == null){
            return Result.fail("无智运宝账户");
        }
        return Result.success(accountDto);
    }

    @Override
    public Result increaseBalance(String userId, BigDecimal amount) {
        int rows = this.baseMapper.rechargeBalance(userId, amount);
        if(rows != 1){
            log.error("账户充值更新失败: userId={}, amount={}, rows={}", userId, amount, rows);
            return Result.fail("充值失败：账户不存在或状态异常");
        }
        return Result.success();
    }
}
