package com.monkey.account.bsm.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.monkey.account.bsm.biz.entity.Account;
import org.apache.ibatis.annotations.Mapper;

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

}
