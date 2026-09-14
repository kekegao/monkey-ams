package com.monkey.account.bsm.biz.api;

import com.monkey.account.bsm.biz.dto.IncomeExpensePageDto;
import com.monkey.account.bsm.biz.request.IncomeExpenseQueryRequest;
import com.monkey.ams.common.response.Result;

public interface IncomeExpenseProtocol {

    /**
     * 分页查询当前登录用户的收支流水列表
     *
     * @param request 查询参数（userId、pageNum、pageSize）
     * @return 收支流水分页结果
     */
    Result<IncomeExpensePageDto> queryIncomeExpenseList(IncomeExpenseQueryRequest request);
}
