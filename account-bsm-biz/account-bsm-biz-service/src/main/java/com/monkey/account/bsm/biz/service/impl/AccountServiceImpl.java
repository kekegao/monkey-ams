package com.monkey.account.bsm.biz.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.entity.Account;
import com.monkey.account.bsm.biz.mapper.AccountMapper;
import com.monkey.account.bsm.biz.service.inf.AccountService;
import com.monkey.ams.common.response.Result;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 账户表 服务实现类
 * </p>
 *
 * @author gkk
 * @since 2026-08-26
 */
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
}
