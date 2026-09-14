package com.monkey.account.bsm.biz.protocol;

import com.monkey.account.bsm.biz.api.IncomeExpenseProtocol;
import com.monkey.account.bsm.biz.dto.IncomeExpensePageDto;
import com.monkey.account.bsm.biz.request.IncomeExpenseQueryRequest;
import com.monkey.account.bsm.biz.service.inf.IncomeExpenseService;
import com.monkey.ams.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@DubboService
public class IncomeExpenseProtocolImpl implements IncomeExpenseProtocol {

    @Autowired
    private IncomeExpenseService incomeExpenseService;

    @Override
    public Result<IncomeExpensePageDto> queryIncomeExpenseList(IncomeExpenseQueryRequest request) {
        try {
            if (request == null) {
                request = new IncomeExpenseQueryRequest();
            }
            IncomeExpensePageDto page = incomeExpenseService.queryPage(request);
            return Result.success(page);
        } catch (IllegalArgumentException e) {
            log.warn("收支流水查询参数校验失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("收支流水查询异常", e);
            return Result.fail("收支流水查询失败，请稍后重试");
        }
    }
}
