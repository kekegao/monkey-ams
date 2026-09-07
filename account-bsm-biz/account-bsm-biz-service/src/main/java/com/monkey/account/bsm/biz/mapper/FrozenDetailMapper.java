package com.monkey.account.bsm.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.monkey.account.bsm.biz.entity.FrozenDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 账户冻结明细记录表 Mapper 接口
 * </p>
 *
 * @author gkk
 * @since 2026-09-07
 */
@Mapper
public interface FrozenDetailMapper extends BaseMapper<FrozenDetail> {

}
