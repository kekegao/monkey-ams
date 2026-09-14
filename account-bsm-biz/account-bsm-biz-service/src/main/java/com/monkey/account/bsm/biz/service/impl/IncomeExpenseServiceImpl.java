package com.monkey.account.bsm.biz.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.monkey.account.bsm.biz.dto.IncomeExpenseDto;
import com.monkey.account.bsm.biz.dto.IncomeExpensePageDto;
import com.monkey.account.bsm.biz.entity.IncomeExpense;
import com.monkey.account.bsm.biz.mapper.IncomeExpenseMapper;
import com.monkey.account.bsm.biz.request.IncomeExpenseQueryRequest;
import com.monkey.account.bsm.biz.service.inf.IncomeExpenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 账户收入支出流水表 服务实现类
 * </p>
 *
 * @author gkk
 * @since 2026-09-14
 */
@Slf4j
@Service
public class IncomeExpenseServiceImpl extends ServiceImpl<IncomeExpenseMapper, IncomeExpense> implements IncomeExpenseService {

    private static final byte DELETE_FLAG_NO = 0;

    @Override
    public IncomeExpense recordFlow(IncomeExpense flow) {
        if (flow == null || flow.getUserId() == null || flow.getBizNo() == null
                || flow.getBizType() == null || flow.getDirection() == null || flow.getAmount() == null) {
            throw new IllegalArgumentException("收支流水参数不完整，拒绝记账");
        }

        // 幂等：同账户 + 同业务类型 + 同业务单号 + 同记账方向 只记一笔
        IncomeExpense exist = lambdaQuery()
                .eq(IncomeExpense::getUserId, flow.getUserId())
                .eq(IncomeExpense::getBizType, flow.getBizType())
                .eq(IncomeExpense::getBizNo, flow.getBizNo())
                .eq(IncomeExpense::getDirection, flow.getDirection())
                .orderByDesc(IncomeExpense::getFlowTime)
                .last("limit 1")
                .one();
        if (exist != null) {
            log.info("收支流水幂等命中，跳过重复记账: flowNo={}, userId={}, bizType={}, bizNo={}, direction={}",
                    exist.getFlowNo(), flow.getUserId(), flow.getBizType(), flow.getBizNo(), flow.getDirection());
            return exist;
        }

        save(flow);
        return flow;
    }

    @Override
    public IncomeExpensePageDto queryPage(IncomeExpenseQueryRequest request) {
        if (request == null || isBlank(request.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 20 : Math.min(request.getPageSize(), 100);

        long total = lambdaQuery()
                .eq(IncomeExpense::getUserId, request.getUserId())
                .eq(IncomeExpense::getDeleteFlag, DELETE_FLAG_NO)
                .count();
        long pages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;

        List<IncomeExpense> records = Collections.emptyList();
        if (total > 0) {
            int offset = (pageNum - 1) * pageSize;
            records = lambdaQuery()
                    .eq(IncomeExpense::getUserId, request.getUserId())
                    .eq(IncomeExpense::getDeleteFlag, DELETE_FLAG_NO)
                    .orderByDesc(IncomeExpense::getFlowTime)
                    .last("LIMIT " + offset + "," + pageSize)
                    .list();
        }

        IncomeExpensePageDto dto = new IncomeExpensePageDto();
        dto.setRecords(records.stream().map(this::toDto).collect(Collectors.toList()));
        dto.setTotal(total);
        dto.setPages(pages);
        dto.setCurrent(pageNum);
        dto.setSize(pageSize);
        return dto;
    }

    private IncomeExpenseDto toDto(IncomeExpense entity) {
        IncomeExpenseDto dto = new IncomeExpenseDto();
        dto.setId(entity.getId());
        dto.setFlowNo(entity.getFlowNo());
        dto.setUserId(entity.getUserId());
        dto.setUserName(entity.getUserName());
        dto.setAccountType(entity.getAccountType());
        dto.setDirection(entity.getDirection());
        dto.setDirectionDesc(entity.getDirectionDesc());
        dto.setBizType(entity.getBizType());
        dto.setBizTypeName(entity.getBizTypeName());
        dto.setIncomeExpenseType(entity.getIncomeExpenseType());
        dto.setIncomeExpenseTypeName(entity.getIncomeExpenseTypeName());
        dto.setAmount(entity.getAmount());
        dto.setBalanceBefore(entity.getBalanceBefore());
        dto.setBalanceAfter(entity.getBalanceAfter());
        dto.setAvailableBefore(entity.getAvailableBefore());
        dto.setAvailableAfter(entity.getAvailableAfter());
        dto.setFrozenBefore(entity.getFrozenBefore());
        dto.setFrozenAfter(entity.getFrozenAfter());
        dto.setCounterpartyUserId(entity.getCounterpartyUserId());
        dto.setCounterpartyName(entity.getCounterpartyName());
        dto.setCounterpartyAccountType(entity.getCounterpartyAccountType());
        dto.setOrderId(entity.getOrderId());
        dto.setBizNo(entity.getBizNo());
        dto.setFlowTime(entity.getFlowTime());
        dto.setStatus(entity.getStatus());
        dto.setStatusDesc(entity.getStatusDesc());
        dto.setRelatedFlowNo(entity.getRelatedFlowNo());
        dto.setRemark(entity.getRemark());
        dto.setDeleteFlag(entity.getDeleteFlag());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCreateName(entity.getCreateName());
        dto.setUpdateName(entity.getUpdateName());
        return dto;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
