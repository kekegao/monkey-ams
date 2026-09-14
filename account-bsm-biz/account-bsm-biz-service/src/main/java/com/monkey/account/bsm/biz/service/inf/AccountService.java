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

    /**
     * 账户出账：余额、可用金额原子递减
     * <p>
     * 使用 SQL 层原子扣减并带「可用余额充足」条件，并发下不会透支；
     * 可用余额不足或账户状态异常时返回失败，调用方不得继续后续入账动作。
     *
     * @param userId 用户ID
     * @param amount 出账金额（须大于 0）
     * @return
     */
    Result deductAvailableBalance(String userId, BigDecimal amount);

    //Result frozenTransportMoneyAccount(String userId, BigDecimal amount);

}
