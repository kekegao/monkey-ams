package com.monkey.ams.app.controller.pps;

import com.monkey.account.bsm.biz.api.IncomeExpenseProtocol;
import com.monkey.account.bsm.biz.dto.IncomeExpensePageDto;
import com.monkey.account.bsm.biz.request.IncomeExpenseQueryRequest;
import com.monkey.ams.app.controller.BaseController;
import com.monkey.ams.common.response.Result;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/incomeExpense")
public class IncomeExpenseController extends BaseController {

    @DubboReference
    private IncomeExpenseProtocol incomeExpenseProtocol;

    /**
     * 查询当前登录用户的收支流水列表
     * <p>
     * 完整调用链：
     * incomeExpenseList.vue -> ams-app(IncomeExpenseController) -> account-bsm-biz-service(IncomeExpenseProtocolImpl)
     *   -> IncomeExpenseServiceImpl -> IncomeExpenseMapper -> MyBatis -> tf_b_income_expense 表
     *
     * POST /incomeExpense/list
     */
    @PostMapping("/list")
    public Result<IncomeExpensePageDto> queryIncomeExpenseList(@RequestBody IncomeExpenseQueryRequest request) {
        if (request == null) {
            request = new IncomeExpenseQueryRequest();
        }
        request.setUserId(getUserId());
        return incomeExpenseProtocol.queryIncomeExpenseList(request);
    }
}
