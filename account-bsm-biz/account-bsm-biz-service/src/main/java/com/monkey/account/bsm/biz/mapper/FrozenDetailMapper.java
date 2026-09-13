package com.monkey.account.bsm.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.monkey.account.bsm.biz.dto.FrozenDetailDto;
import com.monkey.account.bsm.biz.entity.FrozenDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 查询当前用户「冻结中」的冻结明细列表（status=1）
     *
     * @param userId 用户ID
     * @return 冻结明细列表
     */
    List<FrozenDetailDto> selectFrozenDetailList(@Param("userId") String userId);
}
