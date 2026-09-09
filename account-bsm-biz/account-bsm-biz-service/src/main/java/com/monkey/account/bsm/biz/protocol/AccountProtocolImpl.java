package com.monkey.account.bsm.biz.protocol;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.entity.Account;
import com.monkey.account.bsm.biz.entity.FrozenDetail;
import com.monkey.account.bsm.biz.request.FrozenMoneyAccountRequest;
import com.monkey.account.bsm.biz.request.UnFrozenMoneyAccountRequest;
import com.monkey.account.bsm.biz.service.inf.AccountService;
import com.monkey.account.bsm.biz.service.inf.FrozenDetailService;
import com.monkey.ams.common.constants.BizTypeEnum;
import com.monkey.ams.common.response.Result;
import com.monkey.common.lock.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@DubboService
public class AccountProtocolImpl implements AccountProtocol {

    @Autowired
    private AccountService accountService;

    @Autowired
    private FrozenDetailService frozenDetailService;

    @DistributedLock(key = "'open:account:' + #jsonObject['userId']", waitTime = 3, leaseTime = -1)
    @Override
    public Result openAccount(JSONObject jsonObject) {

        Account account = new Account();
        account.setUserId(jsonObject.getString("userId"));
        account.setUserName(jsonObject.getString("userName"));
        account.setMobile(jsonObject.getString("mobile"));
        account.setRealName(jsonObject.getString("realName"));
        account.setCreateName(jsonObject.getString("realName"));
        account.setCreateTime(new Date());
        accountService.save(account);

        return Result.success();
    }

    @DistributedLock(key = "'frozen:transportMoney:account:' + #request.userId", waitTime = 3, leaseTime = -1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result frozenTransportMoneyAccount(FrozenMoneyAccountRequest request) {

        // 参数校验：运费必须大于0
        if(request == null || request.getUserId() == null){
            return Result.fail("请求对象为空");
        }
        if(request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("运费金额必须大于0");
        }

        Result<AccountDto> result = accountService.selectAccount(request.getUserId());
        if(!result.isSuccess()) {
            return result;
        }
        AccountDto accountDto = result.getData();
        if(accountDto.getAvailableAmount() == null || accountDto.getAvailableAmount().compareTo(request.getAmount()) < 0) {
            return Result.fail("可用余额不足，请先去充值");
        }

        // 冻结运费：冻结金额累加运费，可用余额扣减运费
        BigDecimal frozenAmount = accountDto.getFrozenAmount() == null
                ? BigDecimal.ZERO : accountDto.getFrozenAmount();
        BigDecimal newFrozenAmount = frozenAmount.add(request.getAmount());
        BigDecimal newAvailableAmount = accountDto.getAvailableAmount().subtract(request.getAmount());

        Account account = new Account();
        account.setId(accountDto.getId());
        account.setFrozenAmount(newFrozenAmount);
        account.setAvailableAmount(newAvailableAmount);
        account.setUpdateTime(new Date());
        account.setUpdateName(accountDto.getRealName());
        accountService.updateById(account);

        FrozenDetail frozenDetail = new FrozenDetail();
        frozenDetail.setUserId(accountDto.getUserId());
        frozenDetail.setUserName(accountDto.getUserName());
        frozenDetail.setFrozenNo(request.getFrozenNo());
        frozenDetail.setBizType(request.getBizType());
        frozenDetail.setBizTypeName(BizTypeEnum.getByValue(request.getBizType()).getName());
        frozenDetail.setOrderNo(request.getOrderNo());
        frozenDetail.setAmount(request.getAmount());
        frozenDetail.setFrozenTime(new Date());
        frozenDetail.setStatus(1);
        frozenDetail.setStatusDesc("冻结中");
        frozenDetail.setRemark("发布冻结");
        frozenDetail.setCreateTime(new Date());
        frozenDetail.setCreateName(accountDto.getRealName());
        frozenDetailService.save(frozenDetail);

        log.info("冻结运费成功: userId={}, amount={}, frozenAmount={}, availableAmount={}",
                request.getUserId(), request.getAmount(), newFrozenAmount, newAvailableAmount);

        return Result.success();
    }

    @DistributedLock(key = "'frozen:transportMoney:account:' + #request.userId", waitTime = 3, leaseTime = -1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result unfrozenTransportMoneyAccount(UnFrozenMoneyAccountRequest request) {

        if(request == null || request.getUserId() == null){
            return Result.fail("请求对象为空");
        }
        // 参数校验：解冻运费必须大于0
        if(request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("解冻运费金额必须大于0");
        }

        Result<AccountDto> result = accountService.selectAccount(request.getUserId());
        if(!result.isSuccess()) {
            return result;
        }
        AccountDto accountDto = result.getData();

        // 校验冻结余额是否足够释放
        BigDecimal frozenAmount = accountDto.getFrozenAmount() == null
                ? BigDecimal.ZERO : accountDto.getFrozenAmount();
        if(frozenAmount.compareTo(request.getAmount()) < 0) {
            return Result.fail("冻结金额不足，无法释放");
        }

        FrozenDetail frozenDetail = frozenDetailService.lambdaQuery()
                .eq(FrozenDetail::getUserId, request.getUserId())
                .eq(FrozenDetail::getFrozenNo, request.getFrozenNo())
                .eq(FrozenDetail::getStatus,1)
                .one();
        if(frozenDetail == null) {
            return Result.fail("无冻结记录，请核实!");
        }

        FrozenDetail updateFrozenDetail = new FrozenDetail();
        updateFrozenDetail.setId(frozenDetail.getId());
        updateFrozenDetail.setStatus(2);
        updateFrozenDetail.setStatusDesc("已解冻");
        updateFrozenDetail.setFinishTime(new Date());
        updateFrozenDetail.setUpdateTime(new Date());
        updateFrozenDetail.setUpdateName(accountDto.getRealName());
        frozenDetailService.updateById(updateFrozenDetail);


        // 释放运费：冻结金额扣减运费，可用余额加回运费
        BigDecimal newFrozenAmount = frozenAmount.subtract(request.getAmount());
        BigDecimal newAvailableAmount = accountDto.getAvailableAmount().add(request.getAmount());

        Account account = new Account();
        account.setId(accountDto.getId());
        account.setFrozenAmount(newFrozenAmount);
        account.setAvailableAmount(newAvailableAmount);
        account.setUpdateTime(new Date());
        account.setUpdateName(accountDto.getRealName());
        accountService.updateById(account);

        log.info("释放运费成功: userId={}, amount={}, frozenAmount={}, availableAmount={}",
                request.getUserId(), request.getAmount(), newFrozenAmount, newAvailableAmount);

        return Result.success();
    }

    /**
     * 冻结承运方发货保证金
     *
     * @param request
     * @return
     */
    @DistributedLock(key = "'frozen:carrierMoney:account:' + #request.userId", waitTime = 3, leaseTime = -1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result frozenCarrierMoneyAccount(FrozenMoneyAccountRequest request) {
        // 参数校验：运费必须大于0
        if(request == null || request.getUserId() == null){
            return Result.fail("请求对象为空");
        }
        if(request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("运费金额必须大于0");
        }

        Result<AccountDto> result = accountService.selectAccount(request.getUserId());
        if(!result.isSuccess()) {
            return result;
        }
        AccountDto accountDto = result.getData();
        if(accountDto.getAvailableAmount() == null || accountDto.getAvailableAmount().compareTo(request.getAmount()) < 0) {
            return Result.fail("可用余额不足，请先去充值");
        }

        // 冻结发货保证金：冻结金额累加保证金，可用余额扣减保证金
        BigDecimal frozenAmount = accountDto.getFrozenAmount() == null
                ? BigDecimal.ZERO : accountDto.getFrozenAmount();
        BigDecimal newFrozenAmount = frozenAmount.add(request.getAmount());
        BigDecimal newAvailableAmount = accountDto.getAvailableAmount().subtract(request.getAmount());

        Account account = new Account();
        account.setId(accountDto.getId());
        account.setFrozenAmount(newFrozenAmount);
        account.setAvailableAmount(newAvailableAmount);
        account.setUpdateTime(new Date());
        account.setUpdateName(accountDto.getRealName());
        accountService.updateById(account);

        FrozenDetail frozenDetail = new FrozenDetail();
        frozenDetail.setUserId(accountDto.getUserId());
        frozenDetail.setUserName(accountDto.getUserName());
        frozenDetail.setFrozenNo(request.getFrozenNo());
        frozenDetail.setBizType(request.getBizType());
        frozenDetail.setBizTypeName(BizTypeEnum.getByValue(request.getBizType()).getName());
        frozenDetail.setOrderNo(request.getOrderNo());
        frozenDetail.setAmount(request.getAmount());
        frozenDetail.setFrozenTime(new Date());
        frozenDetail.setStatus(1);
        frozenDetail.setStatusDesc("冻结中");
        frozenDetail.setRemark("发货保证金冻结");
        frozenDetail.setCreateTime(new Date());
        frozenDetail.setCreateName(accountDto.getRealName());
        frozenDetailService.save(frozenDetail);

        log.info("确认发货保证金冻结成功: userId={}, amount={}, frozenAmount={}, availableAmount={}",
                request.getUserId(), request.getAmount(), newFrozenAmount, newAvailableAmount);

        return Result.success();
    }

    /**
     * 释放承运方保证金
     *
     * @param request
     * @return
     */
    @DistributedLock(key = "'frozen:carrierMoney:account:' + #request.userId", waitTime = 3, leaseTime = -1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result unFrozenCarrierMoneyAccount(UnFrozenMoneyAccountRequest request) {
        if(request == null || request.getUserId() == null){
            return Result.fail("请求对象为空");
        }
        // 参数校验：解冻发货保证金必须大于0
        if(request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("发货保证金金额必须大于0");
        }

        Result<AccountDto> result = accountService.selectAccount(request.getUserId());
        if(!result.isSuccess()) {
            return result;
        }
        AccountDto accountDto = result.getData();

        // 校验冻结余额是否足够释放
        BigDecimal frozenAmount = accountDto.getFrozenAmount() == null
                ? BigDecimal.ZERO : accountDto.getFrozenAmount();
        if(frozenAmount.compareTo(request.getAmount()) < 0) {
            return Result.fail("冻结金额不足，无法释放");
        }

        FrozenDetail frozenDetail = frozenDetailService.lambdaQuery()
                .eq(FrozenDetail::getUserId, request.getUserId())
                .eq(FrozenDetail::getFrozenNo, request.getFrozenNo())
                .eq(FrozenDetail::getStatus,1)
                .one();
        if(frozenDetail == null) {
            return Result.fail("无冻结记录，请核实!");
        }

        FrozenDetail updateFrozenDetail = new FrozenDetail();
        updateFrozenDetail.setId(frozenDetail.getId());
        updateFrozenDetail.setStatus(2);
        updateFrozenDetail.setStatusDesc("已解冻");
        updateFrozenDetail.setFinishTime(new Date());
        updateFrozenDetail.setUpdateTime(new Date());
        updateFrozenDetail.setUpdateName(accountDto.getRealName());
        frozenDetailService.updateById(updateFrozenDetail);


        // 释放发货保证金：冻结金额扣减保证金，可用余额加回保证金
        BigDecimal newFrozenAmount = frozenAmount.subtract(request.getAmount());
        BigDecimal newAvailableAmount = accountDto.getAvailableAmount().add(request.getAmount());

        Account account = new Account();
        account.setId(accountDto.getId());
        account.setFrozenAmount(newFrozenAmount);
        account.setAvailableAmount(newAvailableAmount);
        account.setUpdateTime(new Date());
        account.setUpdateName(accountDto.getRealName());
        accountService.updateById(account);

        log.info("释放发货保证金成功: userId={}, amount={}, frozenAmount={}, availableAmount={}",
                request.getUserId(), request.getAmount(), newFrozenAmount, newAvailableAmount);

        return Result.success();
    }

    @Override
    public Result<AccountDto> selectAccount(String userId) {
        return accountService.selectAccount(userId);
    }
}
