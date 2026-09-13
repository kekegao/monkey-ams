package com.monkey.account.bsm.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.entity.Account;
import com.monkey.ams.common.response.Result;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * <p>
 * 账户表 Mapper 接口
 * </p>
 *
 * @author gkk
 * @since 2026-08-26
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /**
     * 查询智运宝账户
     *
     * @param userId
     * @return
     */
    AccountDto selectAccount(@Param("userId") String userId);

    /**
     * 充值：余额、可用金额原子累加（SQL 层加法，避免并发覆盖）
     *
     * @param userId 用户ID
     * @param amount 充值金额
     * @return 影响行数
     */
    int rechargeBalance(@Param("userId") String userId, @Param("amount") BigDecimal amount);

}
