package com.monkey.account.bsm.biz.protocol;

import com.monkey.account.bsm.biz.api.AccountRechargeProtocol;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.request.RechargeAccountRequest;
import com.monkey.account.bsm.biz.service.inf.AccountService;
import com.monkey.ams.common.response.Result;
import com.monkey.common.lock.annotation.DistributedLock;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Slf4j
@DubboService
public class AccountRechargeProtocolImpl implements AccountRechargeProtocol {

    @Resource
    private AccountService accountService;
    /**
     * 充值
     *
     * 设计说明：
     * 1. 通过 @DistributedLock 按 userId 串行化充值请求，避免同一账户并发重复充值；
     * 2. 余额更新使用 SQL 层原子累加（balance = balance + amount），
     *    而非「查询-计算-整体覆盖」，从根本上避免并发丢失更新，保证数据一致性；
     * 3. @Transactional 兜底事务，便于后续扩展充值流水等操作整体回滚。
     *
     * @param request
     * @return
     */
    @DistributedLock(key = "'account:recharge:' + #request.userId", waitTime = 3, leaseTime = -1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result recharge(RechargeAccountRequest request) {
        // 1. 参数校验
        if(request == null || !StringUtils.hasText(request.getUserId())) {
            return Result.fail("用户ID不能为空");
        }
        if(request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("充值金额必须大于0");
        }

        // 2. 查询账户，确保账户存在且状态正常
        Result<AccountDto> result = accountService.selectAccount(request.getUserId());
        if(!result.isSuccess()) {
            return result;
        }
        AccountDto accountDto = result.getData();
        if(accountDto.getDeleteFlag() != null && accountDto.getDeleteFlag() == 1) {
            return Result.fail("账户已注销，无法充值");
        }

        // 3. 余额、可用金额原子累加
        Result rechargeResult = accountService.increaseBalance(request.getUserId(), request.getAmount());
        if(!rechargeResult.isSuccess()) {
            return rechargeResult;
        }

        // 4. 回查最新账户信息返回给调用方
        Result<AccountDto> latestResult = accountService.selectAccount(request.getUserId());
        AccountDto latest = latestResult.isSuccess() ? latestResult.getData() : accountDto;

        log.info("账户充值成功: userId={}, amount={}, balance={}, availableAmount={}",
                request.getUserId(), request.getAmount(),
                latest.getBalance(), latest.getAvailableAmount());

        return Result.success(latest);
    }
}
