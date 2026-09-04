package com.monkey.account.bsm.biz.service.inf;


import com.baomidou.mybatisplus.spring.service.IService;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.entity.Account;
import com.monkey.ams.common.response.Result;

import java.math.BigDecimal;

/**
 * <p>
 * 账户表 服务类
 * </p>
 *
 * @author gkk
 * @since 2026-08-26
 */
public interface AccountService extends IService<Account> {

    /**
     * 查询智运宝账户
     *
     * @param userId
     * @return
     */
    Result<AccountDto> selectAccount(String userId);

    /**
     * 账户充值：余额、可用金额原子累加
     *
     * @param userId 用户ID
     * @param amount 充值金额
     * @return
     */
    Result increaseBalance(String userId, BigDecimal amount);


    //Result frozenTransportMoneyAccount(String userId, BigDecimal amount);

}
