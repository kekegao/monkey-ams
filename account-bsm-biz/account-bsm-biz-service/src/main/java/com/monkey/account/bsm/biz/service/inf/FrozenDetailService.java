package com.monkey.account.bsm.biz.service.inf;


import com.baomidou.mybatisplus.spring.service.IService;
import com.monkey.account.bsm.biz.dto.FrozenDetailDto;
import com.monkey.account.bsm.biz.entity.FrozenDetail;
import com.monkey.ams.common.response.Result;

import java.util.List;

/**
 * <p>
 * 账户冻结明细记录表 服务类
 * </p>
 *
 * @author gkk
 * @since 2026-09-07
 */
public interface FrozenDetailService extends IService<FrozenDetail> {

    /**
     * 查询当前用户「冻结中」的冻结明细列表（status=1）
     *
     * @param userId 用户ID
     * @return 冻结明细列表
     */
    Result<List<FrozenDetailDto>> selectFrozenDetailList(String userId);
}
