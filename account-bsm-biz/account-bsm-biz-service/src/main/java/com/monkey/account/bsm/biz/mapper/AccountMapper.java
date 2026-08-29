package com.monkey.account.bsm.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.entity.Account;
import com.monkey.ams.common.response.Result;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

}
