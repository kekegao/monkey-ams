package com.monkey.account.bsm.biz.service.inf;

import com.baomidou.mybatisplus.spring.service.IService;
import com.monkey.account.bsm.biz.dto.IncomeExpensePageDto;
import com.monkey.account.bsm.biz.entity.IncomeExpense;
import com.monkey.account.bsm.biz.request.IncomeExpenseQueryRequest;

/**
 * <p>
 * 账户收入支出流水表 服务类
 * </p>
 *
 * @author gkk
 * @since 2026-09-14
 */
public interface IncomeExpenseService extends IService<IncomeExpense> {

    /**
     * 记录一条收支流水（幂等）
     * <p>
     * 幂等键与唯一索引 uk_biz_dedup 对齐：同一账户 + 同一业务类型 + 同一业务单号 + 同一记账方向只允许一条流水。
     * 已存在时直接返回既有流水，不重复记账（配合数据库唯一索引双重兜底）。
     * <p>
     * 注意：必须在「账户余额变更」的同一个本地事务内调用，避免出现「有资金无流水」或「有流水无资金」。
     *
     * @param flow 待落库的收支流水（userId、bizType、bizNo、direction、amount 必填）
     * @return 已存在的流水或本次新落库的流水
     */
    IncomeExpense recordFlow(IncomeExpense flow);

    /**
     * 分页查询用户收支流水列表
     *
     * @param request 查询参数
     * @return 分页结果
     */
    IncomeExpensePageDto queryPage(IncomeExpenseQueryRequest request);
}
