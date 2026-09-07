package com.monkey.account.bsm.biz.api;

import com.monkey.account.bsm.biz.dto.FrozenDetailDto;
import com.monkey.ams.common.response.Result;

import java.util.List;

public interface FrozenDetailProtocol {

    /**
     * 查询当前用户「冻结中」的冻结明细列表（status=1）
     *
     * @param userId 用户ID
     * @return 冻结明细列表
     */
    Result<List<FrozenDetailDto>> selectFrozenDetailList(String userId);
}
