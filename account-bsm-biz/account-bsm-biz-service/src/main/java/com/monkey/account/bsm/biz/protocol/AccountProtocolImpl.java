package com.monkey.account.bsm.biz.protocol;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.entity.Account;
import com.monkey.account.bsm.biz.entity.FrozenDetail;
import com.monkey.account.bsm.biz.entity.IncomeExpense;
import com.monkey.account.bsm.biz.request.FrozenMoneyAccountRequest;
import com.monkey.account.bsm.biz.request.SettleCarrierMoneyAccountRequest;
import com.monkey.account.bsm.biz.request.SettleMoneyAccountRequest;
import com.monkey.account.bsm.biz.request.SettleShipperMoneyAccountRequest;
import com.monkey.account.bsm.biz.request.UnFrozenMoneyAccountRequest;
import com.monkey.account.bsm.biz.service.inf.AccountService;
import com.monkey.account.bsm.biz.service.inf.FrozenDetailService;
import com.monkey.account.bsm.biz.service.inf.IncomeExpenseService;
import com.monkey.ams.common.constants.AccountTypeEnum;
import com.monkey.ams.common.constants.BizTypeEnum;
import com.monkey.ams.common.constants.IncomeExpenseBizTypeEnum;
import com.monkey.ams.common.constants.IncomeExpenseDirectionEnum;
import com.monkey.ams.common.constants.IncomeExpenseTypeEnum;
import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.StringGenerateUtil;
import com.monkey.common.lock.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@DubboService
public class AccountProtocolImpl implements AccountProtocol {

    @Autowired
    private AccountService accountService;

    @Autowired
    private FrozenDetailService frozenDetailService;

    @Autowired
    private IncomeExpenseService incomeExpenseService;

    /**
     * 平台公司对公账户用户ID（货主清算收款账户，由配置中心下发；未配置时货主清算直接失败，避免资金流向不确定账户）
     */
    @Value("${account.platform.company-user-id:}")
    private String platformCompanyUserId;

    /**
     * 单笔划账金额上限（1 亿）：与 settlement 清算金额上限保持一致，用于拦截异常入参
     */
    private static final BigDecimal MAX_SETTLE_AMOUNT = new BigDecimal("100000000");

    /**
     * 收支流水号前缀（SZ + 时间戳 + 随机数）
     */
    private static final String INCOME_EXPENSE_FLOW_NO_PREFIX = "SZ";

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

        // 收支流水：运费托管冻结（可用余额 -> 冻结金额，属资金形态变化，不计入收支科目）
        IncomeExpense frozenFlow = new IncomeExpense()
                .setDirection(IncomeExpenseDirectionEnum.FROZEN.byteValue())
                .setDirectionDesc(IncomeExpenseDirectionEnum.FROZEN.getName())
                .setBizType(IncomeExpenseBizTypeEnum.TRANSPORT_MONEY.byteValue())
                .setBizTypeName(IncomeExpenseBizTypeEnum.TRANSPORT_MONEY.getName())
                .setIncomeExpenseType(IncomeExpenseTypeEnum.NOT_APPLICABLE.byteValue())
                .setIncomeExpenseTypeName(IncomeExpenseTypeEnum.NOT_APPLICABLE.getName())
                .setAmount(request.getAmount())
                .setOrderId(request.getOrderNo())
                .setBizNo(request.getFrozenNo())
                .setRemark("运费托管冻结")
                .setCreateName(accountDto.getRealName());
        fillAfterSnapshot(frozenFlow, null, accountDto, BigDecimal.ZERO,
                request.getAmount().negate(), request.getAmount());
        recordIncomeExpense(accountDto, frozenFlow);

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

        // 收支流水：运费托管解冻（冻结金额 -> 可用余额，属资金形态变化，不计入收支科目）
        IncomeExpense unfrozenFlow = new IncomeExpense()
                .setDirection(IncomeExpenseDirectionEnum.UNFROZEN.byteValue())
                .setDirectionDesc(IncomeExpenseDirectionEnum.UNFROZEN.getName())
                .setBizType(IncomeExpenseBizTypeEnum.TRANSPORT_MONEY.byteValue())
                .setBizTypeName(IncomeExpenseBizTypeEnum.TRANSPORT_MONEY.getName())
                .setIncomeExpenseType(IncomeExpenseTypeEnum.NOT_APPLICABLE.byteValue())
                .setIncomeExpenseTypeName(IncomeExpenseTypeEnum.NOT_APPLICABLE.getName())
                .setAmount(request.getAmount())
                .setOrderId(frozenDetail.getOrderNo())
                .setBizNo(frozenDetail.getFrozenNo())
                .setRemark("运费托管解冻")
                .setCreateName(accountDto.getRealName());
        fillAfterSnapshot(unfrozenFlow, null, accountDto, BigDecimal.ZERO,
                request.getAmount(), request.getAmount().negate());
        recordIncomeExpense(accountDto, unfrozenFlow);

        log.info("释放运费成功: userId={}, amount={}, frozenAmount={}, availableAmount={}",
                request.getUserId(), request.getAmount(), newFrozenAmount, newAvailableAmount);

        return Result.success();
    }

    /**
     * 运费托管结算（对账扣划）：将货主托管运费扣划，并按清算单金额入账承运方。
     * <p>
     * 安全与幂等：
     * 1) 身份与金额均由 settlement 模块按运单快照计算后传入，账户侧仅做原子扣划/入账；
     * 2) 按冻结流水号定位「冻结中」明细，已打款(status=3)时直接幂等返回成功；
     * 3) 分布式锁按货主用户串行化，本地事务保证「货主冻结扣减 + 货主服务费扣减（如有） +
     *    承运方余额入账 + 冻结明细置为已打款」四方一致；
     * 4) 承运方入账使用原子 UPDATE（balance + amount, available_amount + amount），
     *    与后续充值/提现等并发操作行锁互斥但无 select-for-update 长事务。
     *
     * @param request 结算请求
     * @return 结算结果
     * @deprecated 承运方对账已改为「平台公司对公账户 -> 承运方账户」划账，本方法不再被业务调用，
     * 保留仅作历史兼容，请使用 {@link #settleCarrierMoneyFromCompanyAccount(SettleCarrierMoneyAccountRequest)}
     */
    @Deprecated
    @DistributedLock(key = "'settle:transportMoney:account:' + #request.shipperUserId", waitTime = 3, leaseTime = -1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result settleTransportMoneyAccount(SettleMoneyAccountRequest request) {
        if (request == null || request.getShipperUserId() == null || request.getShipperUserId().trim().isEmpty()) {
            return Result.fail("请求对象或货主用户ID为空");
        }
        if (request.getCarrierUserId() == null || request.getCarrierUserId().trim().isEmpty()) {
            return Result.fail("承运方用户ID为空");
        }
        if (request.getOrderNo() == null || request.getOrderNo().trim().isEmpty()) {
            return Result.fail("运单号为空");
        }
        if (request.getFrozenNo() == null || request.getFrozenNo().trim().isEmpty()) {
            return Result.fail("冻结流水号为空");
        }
        if (request.getFrozenAmount() == null || request.getFrozenAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("托管冻结金额必须大于0");
        }
        if (request.getCarrierSettleAmount() == null || request.getCarrierSettleAmount().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail("承运方结算金额异常");
        }

        // 查询货主账户
        Result<AccountDto> shipperResult = accountService.selectAccount(request.getShipperUserId());
        if (!shipperResult.isSuccess()) {
            return shipperResult;
        }
        AccountDto shipperAccount = shipperResult.getData();

        // 定位运费托管冻结明细（状态=1 冻结中）
        FrozenDetail frozenDetail = frozenDetailService.lambdaQuery()
                .eq(FrozenDetail::getUserId, request.getShipperUserId())
                .eq(FrozenDetail::getFrozenNo, request.getFrozenNo())
                .eq(FrozenDetail::getOrderNo, request.getOrderNo())
                .eq(FrozenDetail::getBizType, BizTypeEnum.TRANSPORT_MONEY.getValue())
                .eq(FrozenDetail::getStatus, 1)
                .one();

        // 幂等：已打款则直接返回成功
        if (frozenDetail == null) {
            FrozenDetail settledDetail = frozenDetailService.lambdaQuery()
                    .eq(FrozenDetail::getUserId, request.getShipperUserId())
                    .eq(FrozenDetail::getFrozenNo, request.getFrozenNo())
                    .eq(FrozenDetail::getOrderNo, request.getOrderNo())
                    .eq(FrozenDetail::getBizType, BizTypeEnum.TRANSPORT_MONEY.getValue())
                    .eq(FrozenDetail::getStatus, 3)
                    .one();
            if (settledDetail != null) {
                log.info("运费托管结算幂等命中：冻结明细已打款, orderNo={}, frozenNo={}",
                        request.getOrderNo(), request.getFrozenNo());
                return Result.success();
            }
            return Result.fail("无冻结记录或冻结状态异常");
        }

        // 冻结金额以账户侧实际冻结明细为准（资金唯一真相源）
        BigDecimal actualFrozenAmount = frozenDetail.getAmount() != null ? frozenDetail.getAmount() : BigDecimal.ZERO;
        if (actualFrozenAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("冻结金额异常，无法结算");
        }
        if (actualFrozenAmount.compareTo(request.getFrozenAmount()) != 0) {
            log.warn("结算请求冻结金额与账户实际冻结金额不一致，以账户实际冻结为准: orderNo={}, requestFrozenAmount={}, actualFrozenAmount={}",
                    request.getOrderNo(), request.getFrozenAmount(), actualFrozenAmount);
        }

        // 校验货主冻结余额足够
        BigDecimal shipperFrozen = shipperAccount.getFrozenAmount() != null
                ? shipperAccount.getFrozenAmount() : BigDecimal.ZERO;
        if (shipperFrozen.compareTo(actualFrozenAmount) < 0) {
            log.warn("货主冻结余额不足，无法结算: orderNo={}, shipperUserId={}, frozenAmount={}",
                    request.getOrderNo(), request.getShipperUserId(), actualFrozenAmount);
            return Result.fail("货主冻结余额不足，无法结算");
        }

        // 若存在货主服务费，需从可用余额额外扣划（默认 0）
        BigDecimal shipperServiceFee = request.getShipperServiceFee() != null ? request.getShipperServiceFee() : BigDecimal.ZERO;
        if (shipperServiceFee.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal available = shipperAccount.getAvailableAmount() != null
                    ? shipperAccount.getAvailableAmount() : BigDecimal.ZERO;
            if (available.compareTo(shipperServiceFee) < 0) {
                return Result.fail("货主可用余额不足，无法扣划服务费");
            }
        }

        String operator = request.getOperator();
        Date now = new Date();

        // 1) 扣减货主冻结金额及服务费（保持 balance = available + frozen 恒等式）
        BigDecimal oldAvailable = shipperAccount.getAvailableAmount() != null
                ? shipperAccount.getAvailableAmount() : BigDecimal.ZERO;
        BigDecimal oldBalance = shipperAccount.getBalance() != null
                ? shipperAccount.getBalance() : BigDecimal.ZERO;
        BigDecimal newFrozen = shipperFrozen.subtract(actualFrozenAmount);
        BigDecimal newAvailable = oldAvailable.subtract(shipperServiceFee);
        BigDecimal newBalance = oldBalance.subtract(actualFrozenAmount).subtract(shipperServiceFee);

        Account shipperUpdate = new Account();
        shipperUpdate.setId(shipperAccount.getId());
        shipperUpdate.setFrozenAmount(newFrozen);
        shipperUpdate.setAvailableAmount(newAvailable);
        shipperUpdate.setBalance(newBalance);
        shipperUpdate.setUpdateTime(now);
        shipperUpdate.setUpdateName(operator);
        accountService.updateById(shipperUpdate);

        // 2) 冻结明细置为已打款
        FrozenDetail updateFrozenDetail = new FrozenDetail();
        updateFrozenDetail.setId(frozenDetail.getId());
        updateFrozenDetail.setStatus(3);
        updateFrozenDetail.setStatusDesc("已打款");
        updateFrozenDetail.setFinishTime(now);
        updateFrozenDetail.setUpdateTime(now);
        updateFrozenDetail.setUpdateName(operator);
        frozenDetailService.updateById(updateFrozenDetail);

        // 3) 承运方余额原子入账
        if (request.getCarrierSettleAmount().compareTo(BigDecimal.ZERO) > 0) {
            Result carrierResult = accountService.increaseBalance(request.getCarrierUserId(), request.getCarrierSettleAmount());
            if (!carrierResult.isSuccess()) {
                log.error("承运方账户入账失败，回滚结算: orderNo={}, carrierUserId={}, amount={}",
                        request.getOrderNo(), request.getCarrierUserId(), request.getCarrierSettleAmount());
                throw new RuntimeException("承运方账户入账失败：" + carrierResult.getMessage());
            }
        }

        log.info("运费托管结算成功: orderNo={}, frozenNo={}, shipperUserId={}, carrierUserId={}, frozenAmount={}, shipperServiceFee={}, carrierSettleAmount={}",
                request.getOrderNo(), request.getFrozenNo(), request.getShipperUserId(), request.getCarrierUserId(),
                actualFrozenAmount, shipperServiceFee, request.getCarrierSettleAmount());
        return Result.success();
    }

    /**
     * 货主清算：货主托管运费 -> 平台公司对公账户
     * <p>
     * 业务边界：只做货主侧资金动作（冻结扣减 + 平台对公账户入账），不做承运方任何入账。
     * <p>
     * 安全与幂等：目标账户由配置指定不接受外部传入；扣划金额以账户侧冻结明细为准；
     * 冻结明细已打款时幂等返回；分布式锁 + 本地事务保证多方资金变更原子一致。
     *
     * @param request 清算请求（shipperUserId、orderNo 必填，frozenNo 可为空）
     * @return 实际从托管冻结中扣划的金额
     */
    @DistributedLock(key = "'settle:shipperMoney:company:' + #request.shipperUserId", waitTime = 3, leaseTime = -1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result<BigDecimal> settleShipperMoneyToCompanyAccount(SettleShipperMoneyAccountRequest request) {
        // 1. 入参校验
        if (request == null || request.getShipperUserId() == null || request.getShipperUserId().trim().isEmpty()) {
            return Result.fail("请求对象或货主用户ID为空");
        }
        if (request.getOrderNo() == null || request.getOrderNo().trim().isEmpty()) {
            return Result.fail("运单号为空");
        }
        BigDecimal requestFrozenAmount = request.getFrozenAmount() == null ? BigDecimal.ZERO : request.getFrozenAmount();
        if (requestFrozenAmount.compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail("托管冻结金额异常");
        }
        BigDecimal shipperServiceFee = request.getShipperServiceFee() == null ? BigDecimal.ZERO : request.getShipperServiceFee();
        if (shipperServiceFee.compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail("货主服务费异常");
        }

        // 2. 平台公司对公账户：必须已配置且已开户，否则拒绝资金动作（避免资金流向不确定账户）
        String companyUserId = platformCompanyUserId == null ? "" : platformCompanyUserId.trim();
        if (companyUserId.isEmpty()) {
            log.error("未配置平台公司对公账户，货主清算终止: orderNo={}", request.getOrderNo());
            return Result.fail("未配置平台公司对公账户，无法完成清算");
        }
        Result<AccountDto> companyResult = accountService.selectAccount(companyUserId);
        if (!companyResult.isSuccess() || companyResult.getData() == null) {
            log.error("平台公司对公账户不存在，货主清算终止: orderNo={}, companyUserId={}",
                    request.getOrderNo(), companyUserId);
            return Result.fail("平台公司对公账户不存在，请先开户");
        }
        AccountDto companyAccount = companyResult.getData();

        // 3. 货主账户
        Result<AccountDto> shipperResult = accountService.selectAccount(request.getShipperUserId());
        if (!shipperResult.isSuccess()) {
            return Result.fail(shipperResult.getMessage());
        }
        AccountDto shipperAccount = shipperResult.getData();

        // 4. 定位运费托管冻结明细（幂等锚点：frozenNo 优先，缺省按 货主+运单号+运费托管 兜底）
        FrozenDetail frozenDetail = queryTransportFrozenDetail(
                request.getShipperUserId(), request.getOrderNo(), request.getFrozenNo(), 1);
        if (frozenDetail == null) {
            // 幂等：已打款则直接返回已扣划金额，绝不重复扣款
            FrozenDetail settledDetail = queryTransportFrozenDetail(
                    request.getShipperUserId(), request.getOrderNo(), request.getFrozenNo(), 3);
            if (settledDetail != null) {
                BigDecimal settledAmount = settledDetail.getAmount() != null ? settledDetail.getAmount() : BigDecimal.ZERO;
                log.info("货主清算幂等命中：托管冻结明细已打款, orderNo={}, frozenNo={}, settledAmount={}",
                        request.getOrderNo(), settledDetail.getFrozenNo(), settledAmount);
                return Result.success(settledAmount);
            }
            return Result.fail("无托管冻结记录或冻结状态异常，无法清算");
        }

        // 扣划金额以账户侧实际冻结明细为准（资金唯一真相源）
        BigDecimal actualFrozenAmount = frozenDetail.getAmount() != null ? frozenDetail.getAmount() : BigDecimal.ZERO;
        if (actualFrozenAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("托管冻结金额异常，无法清算");
        }
        if (requestFrozenAmount.compareTo(BigDecimal.ZERO) > 0 && actualFrozenAmount.compareTo(requestFrozenAmount) != 0) {
            log.warn("清算请求冻结金额与账户实际冻结金额不一致，以账户实际冻结为准: orderNo={}, requestFrozenAmount={}, actualFrozenAmount={}",
                    request.getOrderNo(), requestFrozenAmount, actualFrozenAmount);
        }

        // 5. 余额校验：冻结额须足够，服务费须可用余额足够
        BigDecimal shipperFrozen = shipperAccount.getFrozenAmount() != null
                ? shipperAccount.getFrozenAmount() : BigDecimal.ZERO;
        if (shipperFrozen.compareTo(actualFrozenAmount) < 0) {
            log.warn("货主冻结余额不足，无法清算: orderNo={}, shipperUserId={}, frozenAmount={}",
                    request.getOrderNo(), request.getShipperUserId(), actualFrozenAmount);
            return Result.fail("货主冻结余额不足，无法清算");
        }
        BigDecimal oldAvailable = shipperAccount.getAvailableAmount() != null
                ? shipperAccount.getAvailableAmount() : BigDecimal.ZERO;
        if (shipperServiceFee.compareTo(BigDecimal.ZERO) > 0 && oldAvailable.compareTo(shipperServiceFee) < 0) {
            return Result.fail("货主可用余额不足，无法扣划服务费");
        }

        String operator = request.getOperator();
        Date now = new Date();

        // 6. 扣减货主冻结金额及服务费（保持 balance = available + frozen 恒等式）
        BigDecimal oldBalance = shipperAccount.getBalance() != null ? shipperAccount.getBalance() : BigDecimal.ZERO;
        Account shipperUpdate = new Account();
        shipperUpdate.setId(shipperAccount.getId());
        shipperUpdate.setFrozenAmount(shipperFrozen.subtract(actualFrozenAmount));
        shipperUpdate.setAvailableAmount(oldAvailable.subtract(shipperServiceFee));
        shipperUpdate.setBalance(oldBalance.subtract(actualFrozenAmount).subtract(shipperServiceFee));
        shipperUpdate.setUpdateTime(now);
        shipperUpdate.setUpdateName(operator);
        accountService.updateById(shipperUpdate);

        // 7. 冻结明细置为已打款
        FrozenDetail updateFrozenDetail = new FrozenDetail();
        updateFrozenDetail.setId(frozenDetail.getId());
        updateFrozenDetail.setStatus(3);
        updateFrozenDetail.setStatusDesc("已打款");
        updateFrozenDetail.setFinishTime(now);
        updateFrozenDetail.setUpdateTime(now);
        updateFrozenDetail.setUpdateName(operator);
        frozenDetailService.updateById(updateFrozenDetail);

        // 8. 平台公司对公账户原子入账：实际扣划冻结额 + 货主服务费
        BigDecimal companyAmount = actualFrozenAmount.add(shipperServiceFee);
        if (companyAmount.compareTo(BigDecimal.ZERO) > 0) {
            Result companyIncreaseResult = accountService.increaseBalance(companyUserId, companyAmount);
            if (!companyIncreaseResult.isSuccess()) {
                log.error("平台公司对公账户入账失败，回滚货主清算: orderNo={}, companyUserId={}, amount={}",
                        request.getOrderNo(), companyUserId, companyAmount);
                throw new RuntimeException("平台公司对公账户入账失败：" + companyIncreaseResult.getMessage());
            }
        }

        // 9. 收支流水（货主支出 / 平台公司收入，双向配对，与资金变更同事务落库）
        String settleRemark = "托管运费" + actualFrozenAmount + "元，货主服务费" + shipperServiceFee + "元";
        // 9.1 货主侧支出：托管冻结扣划 + 货主服务费，余额减少额等于本次支出额
        IncomeExpense shipperFlow = new IncomeExpense()
                .setDirection(IncomeExpenseDirectionEnum.EXPENSE.byteValue())
                .setDirectionDesc(IncomeExpenseDirectionEnum.EXPENSE.getName())
                .setBizType(IncomeExpenseBizTypeEnum.SHIPPER_SETTLE.byteValue())
                .setBizTypeName(IncomeExpenseBizTypeEnum.SHIPPER_SETTLE.getName())
                .setIncomeExpenseType(IncomeExpenseTypeEnum.TRANSPORT_EXPENSE.byteValue())
                .setIncomeExpenseTypeName(IncomeExpenseTypeEnum.TRANSPORT_EXPENSE.getName())
                .setAmount(companyAmount)
                .setCounterpartyUserId(companyUserId)
                .setCounterpartyName(companyAccount.getUserName())
                .setCounterpartyAccountType(AccountTypeEnum.PLATFORM_COMPANY.byteValue())
                .setOrderId(request.getOrderNo())
                .setBizNo(frozenDetail.getFrozenNo())
                .setRemark("货主清算扣划至平台公司对公账户（" + settleRemark + "）")
                .setCreateName(operator);
        fillAfterSnapshot(shipperFlow, null, shipperAccount, companyAmount.negate(),
                shipperServiceFee.negate(), actualFrozenAmount.negate());
        recordIncomeExpense(shipperAccount, shipperFlow);

        // 9.2 平台公司侧收入：托管运费 + 货主服务费
        IncomeExpense companyFlow = new IncomeExpense()
                .setDirection(IncomeExpenseDirectionEnum.INCOME.byteValue())
                .setDirectionDesc(IncomeExpenseDirectionEnum.INCOME.getName())
                .setBizType(IncomeExpenseBizTypeEnum.SHIPPER_SETTLE.byteValue())
                .setBizTypeName(IncomeExpenseBizTypeEnum.SHIPPER_SETTLE.getName())
                .setIncomeExpenseType(IncomeExpenseTypeEnum.TRANSPORT_INCOME.byteValue())
                .setIncomeExpenseTypeName(IncomeExpenseTypeEnum.TRANSPORT_INCOME.getName())
                .setAmount(companyAmount)
                .setCounterpartyUserId(request.getShipperUserId())
                .setCounterpartyName(shipperAccount.getUserName())
                .setCounterpartyAccountType(AccountTypeEnum.USER.byteValue())
                .setOrderId(request.getOrderNo())
                .setBizNo(frozenDetail.getFrozenNo())
                .setRemark("货主清算到账（" + settleRemark + "）")
                .setCreateName(operator);
        fillAfterSnapshot(companyFlow, queryAccountSnapshotOrNull(companyUserId), companyAccount,
                companyAmount, companyAmount, BigDecimal.ZERO);
        recordIncomeExpense(companyAccount, companyFlow);

        log.info("货主清算资金处理成功: orderNo={}, frozenNo={}, shipperUserId={}, companyUserId={}, frozenAmount={}, shipperServiceFee={}, companyAmount={}",
                request.getOrderNo(), frozenDetail.getFrozenNo(), request.getShipperUserId(), companyUserId,
                actualFrozenAmount, shipperServiceFee, companyAmount);
        return Result.success(actualFrozenAmount);
    }

    /**
     * 承运方清算（对账）划账：平台公司对公账户 -> 承运方智运宝账户
     * <p>
     * 业务边界：只做平台公司对公账户对承运方的划付，不触碰货主任何资金
     * （货主托管运费已在「货主清算」阶段扣划至平台公司对公账户）。
     * <p>
     * 安全与幂等：
     * 1) 出账账户由配置指定（platformCompanyUserId），不接受调用方传入，避免资金流向不确定账户；
     * 2) 出账使用 SQL 层原子扣减并带「可用余额充足」条件，并发场景下不会透支出账；
     * 3) 幂等锚点为承运方清算单号：同一清算单重复请求命中划账流水（已打款）时直接返回已划金额，绝不重复出账；
     * 4) 分布式锁按清算单号串行化，本地事务保证「平台出账 + 承运方入账 + 划账流水」原子一致。
     *
     * @param request 划账请求（carrierUserId、settlementNo、orderNo、settleAmount 必填）
     * @return 实际划付至承运方账户的金额（元）
     */
    @DistributedLock(key = "'settle:carrierMoney:company:' + #request.settlementNo", waitTime = 3, leaseTime = -1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result<BigDecimal> settleCarrierMoneyFromCompanyAccount(SettleCarrierMoneyAccountRequest request) {
        // 1. 入参校验
        if (request == null || isBlank(request.getCarrierUserId())) {
            return Result.fail("请求对象或承运方用户ID为空");
        }
        if (isBlank(request.getSettlementNo())) {
            return Result.fail("承运方清算单号为空");
        }
        if (isBlank(request.getOrderNo())) {
            return Result.fail("运单号为空");
        }
        BigDecimal settleAmount = request.getSettleAmount();
        if (settleAmount == null || settleAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("划账金额必须大于0");
        }
        if (settleAmount.compareTo(MAX_SETTLE_AMOUNT) > 0) {
            return Result.fail("划账金额超出合理范围");
        }
        String settlementNo = request.getSettlementNo().trim();
        String carrierUserId = request.getCarrierUserId().trim();
        String operator = request.getOperator();

        // 2. 幂等：同一清算单已划账（流水 status=3 已打款）时直接返回已划金额，绝不重复出账
        FrozenDetail paidDetail = queryPlatformTransferDetail(settlementNo, 3);
        if (paidDetail != null) {
            BigDecimal paidAmount = paidDetail.getAmount() == null ? settleAmount : paidDetail.getAmount();
            log.info("承运方对账划账幂等命中：该清算单已出账, settlementNo={}, orderNo={}, paidAmount={}",
                    settlementNo, request.getOrderNo(), paidAmount);
            return Result.success(paidAmount);
        }

        // 3. 平台公司对公账户：必须已配置且已开户，否则拒绝出账
        String companyUserId = platformCompanyUserId == null ? "" : platformCompanyUserId.trim();
        if (companyUserId.isEmpty()) {
            log.error("未配置平台公司对公账户，承运方对账划账终止: settlementNo={}", settlementNo);
            return Result.fail("未配置平台公司对公账户，无法完成划账");
        }
        Result<AccountDto> companyResult = accountService.selectAccount(companyUserId);
        if (!companyResult.isSuccess() || companyResult.getData() == null) {
            log.error("平台公司对公账户不存在，承运方对账划账终止: settlementNo={}, companyUserId={}",
                    settlementNo, companyUserId);
            return Result.fail("平台公司对公账户不存在，请先开户");
        }
        AccountDto companyAccount = companyResult.getData();
        if (companyUserId.equals(carrierUserId)) {
            log.error("承运方账户与平台公司对公账户为同一账户，拒绝划账: settlementNo={}", settlementNo);
            return Result.fail("收款账户非法，无法划账");
        }

        // 4. 承运方账户必须存在（收款方账户由清算单指定，不接受任意账户）
        Result<AccountDto> carrierResult = accountService.selectAccount(carrierUserId);
        if (!carrierResult.isSuccess() || carrierResult.getData() == null) {
            log.error("承运方账户不存在，承运方对账划账终止: settlementNo={}, carrierUserId={}",
                    settlementNo, carrierUserId);
            return Result.fail("承运方智运宝账户不存在，无法划账");
        }
        AccountDto carrierAccount = carrierResult.getData();

        // 5. 平台公司账户可用余额校验（并发兜底由 SQL 条件更新完成）
        BigDecimal companyAvailable = companyAccount.getAvailableAmount() == null
                ? BigDecimal.ZERO : companyAccount.getAvailableAmount();
        if (companyAvailable.compareTo(settleAmount) < 0) {
            log.warn("平台公司对公账户可用余额不足，无法划账: settlementNo={}, orderNo={}, available={}, settleAmount={}",
                    settlementNo, request.getOrderNo(), companyAvailable, settleAmount);
            return Result.fail("平台公司对公账户可用余额不足，无法向承运方划账");
        }

        Date now = new Date();

        // 6. 平台公司账户出账：余额、可用金额原子递减，且要求可用余额充足
        Result<?> deductResult = accountService.deductAvailableBalance(companyUserId, settleAmount);
        if (!deductResult.isSuccess()) {
            return Result.fail("平台公司对公账户出账失败：" + deductResult.getMessage());
        }

        // 7. 承运方账户入账：原子累加；失败抛异常回滚平台出账，绝不产生单边账
        Result<?> carrierIncrease = accountService.increaseBalance(carrierUserId, settleAmount);
        if (!carrierIncrease.isSuccess()) {
            log.error("承运方账户入账失败，回滚划账: settlementNo={}, orderNo={}, carrierUserId={}, amount={}",
                    settlementNo, request.getOrderNo(), carrierUserId, settleAmount);
            throw new RuntimeException("承运方账户入账失败：" + carrierIncrease.getMessage());
        }

        // 8. 划账流水落地（幂等锚点）：与出账/入账同事务，事务回滚则锚点一并回滚，重试可安全重放
        savePlatformTransferDetail(carrierUserId, carrierAccount.getUserName(), settlementNo,
                request.getOrderNo(), settleAmount, operator, now);

        // 9. 收支流水（平台公司支出 / 承运方收入，双向配对，与资金变更同事务落库）
        // 9.1 平台公司侧支出：出账账户余额与可用余额同步递减
        IncomeExpense companyFlow = new IncomeExpense()
                .setDirection(IncomeExpenseDirectionEnum.EXPENSE.byteValue())
                .setDirectionDesc(IncomeExpenseDirectionEnum.EXPENSE.getName())
                .setBizType(IncomeExpenseBizTypeEnum.CARRIER_SETTLE.byteValue())
                .setBizTypeName(IncomeExpenseBizTypeEnum.CARRIER_SETTLE.getName())
                .setIncomeExpenseType(IncomeExpenseTypeEnum.TRANSPORT_EXPENSE.byteValue())
                .setIncomeExpenseTypeName(IncomeExpenseTypeEnum.TRANSPORT_EXPENSE.getName())
                .setAmount(settleAmount)
                .setCounterpartyUserId(carrierUserId)
                .setCounterpartyName(carrierAccount.getUserName())
                .setCounterpartyAccountType(AccountTypeEnum.USER.byteValue())
                .setOrderId(request.getOrderNo())
                .setBizNo(settlementNo)
                .setRemark("承运方对账划账出账")
                .setCreateName(operator);
        fillAfterSnapshot(companyFlow, queryAccountSnapshotOrNull(companyUserId), companyAccount,
                settleAmount.negate(), settleAmount.negate(), BigDecimal.ZERO);
        recordIncomeExpense(companyAccount, companyFlow);

        // 9.2 承运方侧收入：实收运费入账（运费扣除平台服务费后的金额）
        IncomeExpense carrierFlow = new IncomeExpense()
                .setDirection(IncomeExpenseDirectionEnum.INCOME.byteValue())
                .setDirectionDesc(IncomeExpenseDirectionEnum.INCOME.getName())
                .setBizType(IncomeExpenseBizTypeEnum.CARRIER_SETTLE.byteValue())
                .setBizTypeName(IncomeExpenseBizTypeEnum.CARRIER_SETTLE.getName())
                .setIncomeExpenseType(IncomeExpenseTypeEnum.TRANSPORT_INCOME.byteValue())
                .setIncomeExpenseTypeName(IncomeExpenseTypeEnum.TRANSPORT_INCOME.getName())
                .setAmount(settleAmount)
                .setCounterpartyUserId(companyUserId)
                .setCounterpartyName(companyAccount.getUserName())
                .setCounterpartyAccountType(AccountTypeEnum.PLATFORM_COMPANY.byteValue())
                .setOrderId(request.getOrderNo())
                .setBizNo(settlementNo)
                .setRemark("承运方对账划账入账")
                .setCreateName(operator);
        fillAfterSnapshot(carrierFlow, queryAccountSnapshotOrNull(carrierUserId), carrierAccount,
                settleAmount, settleAmount, BigDecimal.ZERO);
        recordIncomeExpense(carrierAccount, carrierFlow);

        log.info("承运方对账划账成功: settlementNo={}, orderNo={}, companyUserId={}, carrierUserId={}, settleAmount={}",
                settlementNo, request.getOrderNo(), companyUserId, carrierUserId, settleAmount);
        return Result.success(settleAmount);
    }

    /**
     * 查询平台划账流水（幂等锚点）：以「承运方清算单号」为业务键，biz_type=4 平台划账
     *
     * @param settlementNo 承运方清算单号
     * @param status       流水状态（3 已打款）
     */
    private FrozenDetail queryPlatformTransferDetail(String settlementNo, int status) {
        return frozenDetailService.lambdaQuery()
                .eq(FrozenDetail::getFrozenNo, settlementNo)
                .eq(FrozenDetail::getBizType, BizTypeEnum.PLATFORM_TRANSFER.getValue())
                .eq(FrozenDetail::getStatus, status)
                .orderByDesc(FrozenDetail::getFrozenTime)
                .last("limit 1")
                .one();
    }

    /**
     * 落地平台划账流水
     * <p>
     * 说明：本流水用于审计追溯与幂等锚点，只记录「平台公司对公账户 -> 承运方账户」的出账事实，
     * 不修改承运方账户冻结金额（不产生任何冻结动作），故不影响 tf_b_account.frozen_amount 口径。
     */
    private void savePlatformTransferDetail(String carrierUserId, String carrierUserName, String settlementNo,
                                            String orderNo, BigDecimal amount, String operator, Date now) {
        FrozenDetail detail = new FrozenDetail();
        detail.setUserId(carrierUserId);
        detail.setUserName(carrierUserName);
        detail.setFrozenNo(settlementNo);
        detail.setBizType(BizTypeEnum.PLATFORM_TRANSFER.getValue());
        detail.setBizTypeName(BizTypeEnum.PLATFORM_TRANSFER.getName());
        detail.setOrderNo(orderNo);
        detail.setAmount(amount);
        detail.setFrozenTime(now);
        detail.setStatus(3);
        detail.setStatusDesc("已打款");
        detail.setFinishTime(now);
        detail.setRemark("平台公司对公账户划付承运方清算款");
        detail.setDeleteFlag((byte) 0);
        detail.setCreateTime(now);
        detail.setCreateName(operator);
        detail.setUpdateTime(now);
        detail.setUpdateName(operator);
        frozenDetailService.save(detail);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 账户类型判定：配置的平台公司对公账户返回 2，其余为普通用户账户返回 1
     */
    private byte resolveAccountType(String userId) {
        String companyUserId = platformCompanyUserId == null ? "" : platformCompanyUserId.trim();
        return !companyUserId.isEmpty() && companyUserId.equals(userId)
                ? AccountTypeEnum.PLATFORM_COMPANY.byteValue()
                : AccountTypeEnum.USER.byteValue();
    }

    /**
     * 落地收支流水（收入 / 支出 / 冻结 / 解冻）
     * <p>
     * 统一补齐账户「变动前」快照（余额、可用、冻结）与公共字段（流水号、发生时间、成功状态、审计字段）。
     * 调用方只需设置：记账方向、业务类型、收支科目、发生金额、变动后快照、对手方、运单号/业务单号即可。
     * <p>
     * 事务与幂等：必须在「账户余额变更」的同一个本地事务内调用（账户余额与流水要么同时生效、要么同时回滚），
     * 落库时按「账户 + 业务类型 + 业务单号 + 记账方向」幂等去重，重复消息不会产生重复流水。
     *
     * @param accountBefore 账户变动前快照（由调用方在余额更新前查出，保证快照真实）
     * @param flow          收支流水（变动后快照、金额、方向、业务类型、科目、业务单号等由调用方填充）
     */
    private void recordIncomeExpense(AccountDto accountBefore, IncomeExpense flow) {
        if (accountBefore == null || flow == null) {
            log.warn("收支流水参数缺失，跳过记账: accountBefore={}, flow={}", accountBefore, flow);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        flow.setUserId(accountBefore.getUserId())
                .setUserName(accountBefore.getUserName())
                .setBalanceBefore(nz(accountBefore.getBalance()))
                .setAvailableBefore(nz(accountBefore.getAvailableAmount()))
                .setFrozenBefore(nz(accountBefore.getFrozenAmount()));

        if (flow.getAccountType() == null) {
            flow.setAccountType(resolveAccountType(accountBefore.getUserId()));
        }
        if (isBlank(flow.getFlowNo())) {
            flow.setFlowNo(StringGenerateUtil.generateOrderNo(INCOME_EXPENSE_FLOW_NO_PREFIX));
        }
        if (isBlank(flow.getBizNo())) {
            // 极端兜底：业务单号缺失时退回运单号、再退回流水号，保证幂等键非空（此时失去业务级去重能力）
            flow.setBizNo(isBlank(flow.getOrderId()) ? flow.getFlowNo() : flow.getOrderId());
            log.warn("收支流水业务单号为空，已退回兜底单号: userId={}, direction={}, bizType={}, bizNo={}",
                    flow.getUserId(), flow.getDirection(), flow.getBizType(), flow.getBizNo());
        }
        if (flow.getFlowTime() == null) {
            flow.setFlowTime(now);
        }
        if (flow.getStatus() == null) {
            flow.setStatus((byte) 1);
            flow.setStatusDesc("成功");
        }
        if (flow.getDeleteFlag() == null) {
            flow.setDeleteFlag((byte) 0);
        }
        String operator = isBlank(flow.getCreateName()) ? "system" : flow.getCreateName();
        flow.setCreateName(operator)
                .setUpdateName(operator)
                .setCreateTime(now)
                .setUpdateTime(now);

        IncomeExpense saved = incomeExpenseService.recordFlow(flow);
        log.info("收支流水落库: flowNo={}, userId={}, accountType={}, direction={}, bizType={}, amount={}, "
                        + "balance={}->{}, available={}->{}, frozen={}->{}, orderNo={}, bizNo={}",
                saved.getFlowNo(), saved.getUserId(), saved.getAccountType(), saved.getDirection(), saved.getBizType(),
                saved.getAmount(), saved.getBalanceBefore(), saved.getBalanceAfter(),
                saved.getAvailableBefore(), saved.getAvailableAfter(),
                saved.getFrozenBefore(), saved.getFrozenAfter(), saved.getOrderId(), saved.getBizNo());
    }

    /**
     * 查询账户最新快照（用于流水「变动后」快照落库）
     * <p>
     * 场景：平台公司对公账户 / 承运方账户使用 SQL 原子增减，读取值可能与实际落库值存在并发偏差，
     * 故在余额变更后重新查询一次，保证收支流水的变动后快照与账户真实余额一致。
     *
     * @return 最新账户快照；查询失败返回 null，由调用方按「变动前 + 变动额」兜底
     */
    private AccountDto queryAccountSnapshotOrNull(String userId) {
        try {
            Result<AccountDto> result = accountService.selectAccount(userId);
            if (result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            log.warn("账户最新快照查询失败，收支流水变动后快照改用计算值兜底: userId={}, message={}",
                    userId, result.getMessage());
        } catch (Exception e) {
            log.warn("账户最新快照查询异常，收支流水变动后快照改用计算值兜底: userId={}", userId, e);
        }
        return null;
    }

    /**
     * 回填收支流水的「变动后」快照
     *
     * @param flow           收支流水
     * @param accountAfter   变动后账户快照（可为 null，为 null 时按变动前 + 变动额计算）
     * @param accountBefore  变动前账户快照
     * @param balanceDelta   余额变动额（收入为正、支出为负，冻结/解冻为 0）
     * @param availableDelta 可用余额变动额
     * @param frozenDelta    冻结金额变动额
     */
    private void fillAfterSnapshot(IncomeExpense flow, AccountDto accountAfter, AccountDto accountBefore,
                                   BigDecimal balanceDelta, BigDecimal availableDelta, BigDecimal frozenDelta) {
        if (accountAfter != null) {
            flow.setBalanceAfter(nz(accountAfter.getBalance()))
                    .setAvailableAfter(nz(accountAfter.getAvailableAmount()))
                    .setFrozenAfter(nz(accountAfter.getFrozenAmount()));
            return;
        }
        flow.setBalanceAfter(nz(accountBefore.getBalance()).add(balanceDelta))
                .setAvailableAfter(nz(accountBefore.getAvailableAmount()).add(availableDelta))
                .setFrozenAfter(nz(accountBefore.getFrozenAmount()).add(frozenDelta));
    }

    /**
     * 定位运费托管冻结明细：优先按冻结流水号，缺省按 货主+运单号+业务类型 兜底定位
     *
     * @param shipperUserId 货主用户ID
     * @param orderNo       运单号
     * @param frozenNo      冻结流水号（可为空）
     * @param status        冻结明细状态（1 冻结中 / 3 已打款）
     */
    private FrozenDetail queryTransportFrozenDetail(String shipperUserId, String orderNo, String frozenNo, int status) {
        boolean hasFrozenNo = frozenNo != null && !frozenNo.trim().isEmpty();
        return frozenDetailService.lambdaQuery()
                .eq(FrozenDetail::getUserId, shipperUserId)
                .eq(FrozenDetail::getOrderNo, orderNo)
                .eq(FrozenDetail::getBizType, BizTypeEnum.TRANSPORT_MONEY.getValue())
                .eq(FrozenDetail::getStatus, status)
                .eq(hasFrozenNo, FrozenDetail::getFrozenNo, frozenNo)
                .orderByDesc(FrozenDetail::getFrozenTime)
                .last("limit 1")
                .one();
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

        // 收支流水：发货保证金冻结（可用余额 -> 冻结金额，属资金形态变化，不计入收支科目）
        IncomeExpense frozenFlow = new IncomeExpense()
                .setDirection(IncomeExpenseDirectionEnum.FROZEN.byteValue())
                .setDirectionDesc(IncomeExpenseDirectionEnum.FROZEN.getName())
                .setBizType(IncomeExpenseBizTypeEnum.SHIP_MONEY.byteValue())
                .setBizTypeName(IncomeExpenseBizTypeEnum.SHIP_MONEY.getName())
                .setIncomeExpenseType(IncomeExpenseTypeEnum.NOT_APPLICABLE.byteValue())
                .setIncomeExpenseTypeName(IncomeExpenseTypeEnum.NOT_APPLICABLE.getName())
                .setAmount(request.getAmount())
                .setOrderId(request.getOrderNo())
                .setBizNo(request.getFrozenNo())
                .setRemark("发货保证金冻结")
                .setCreateName(accountDto.getRealName());
        fillAfterSnapshot(frozenFlow, null, accountDto, BigDecimal.ZERO,
                request.getAmount().negate(), request.getAmount());
        recordIncomeExpense(accountDto, frozenFlow);

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
        boolean byFrozenNo = request.getFrozenNo() != null && !request.getFrozenNo().trim().isEmpty();
        // 未指定冻结流水号时，必须能按业务单号定位，避免误释放他人或他单保证金
        if(!byFrozenNo && (request.getOrderNo() == null || request.getOrderNo().trim().isEmpty())) {
            return Result.fail("缺少冻结流水号或关联单号");
        }
        // 参数校验：按流水号释放时解冻发货保证金必须大于0
        if(byFrozenNo && (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            return Result.fail("发货保证金金额必须大于0");
        }

        Result<AccountDto> result = accountService.selectAccount(request.getUserId());
        if(!result.isSuccess()) {
            return result;
        }
        AccountDto accountDto = result.getData();

        // 定位待解冻的冻结明细：优先按冻结流水号；回单确认等未回传流水号的场景，按「业务单号 + 发货保证金」定位
        FrozenDetail frozenDetail;
        if(byFrozenNo) {
            frozenDetail = frozenDetailService.lambdaQuery()
                    .eq(FrozenDetail::getUserId, request.getUserId())
                    .eq(FrozenDetail::getFrozenNo, request.getFrozenNo())
                    .eq(FrozenDetail::getStatus,1)
                    .one();
        } else {
            frozenDetail = frozenDetailService.lambdaQuery()
                    .eq(FrozenDetail::getUserId, request.getUserId())
                    .eq(FrozenDetail::getBizType, BizTypeEnum.SHIP_MONEY.getValue())
                    .eq(FrozenDetail::getOrderNo, request.getOrderNo())
                    .eq(FrozenDetail::getStatus,1)
                    .orderByDesc(FrozenDetail::getFrozenTime)
                    .last("limit 1")
                    .one();
        }
        // 幂等：明细已解冻时查不到「冻结中」记录，重复消息不会重复释放
        if(frozenDetail == null) {
            return Result.fail("无冻结记录，请核实!");
        }

        // 释放金额以冻结明细为准，避免调用方传参与实际冻结金额不一致造成错账
        BigDecimal releaseAmount = frozenDetail.getAmount() != null ? frozenDetail.getAmount() : request.getAmount();
        if(releaseAmount == null || releaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("冻结金额异常，无法释放");
        }

        // 校验冻结余额是否足够释放
        BigDecimal frozenAmount = accountDto.getFrozenAmount() == null
                ? BigDecimal.ZERO : accountDto.getFrozenAmount();
        if(frozenAmount.compareTo(releaseAmount) < 0) {
            return Result.fail("冻结金额不足，无法释放");
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
        BigDecimal newFrozenAmount = frozenAmount.subtract(releaseAmount);
        BigDecimal newAvailableAmount = accountDto.getAvailableAmount().add(releaseAmount);

        Account account = new Account();
        account.setId(accountDto.getId());
        account.setFrozenAmount(newFrozenAmount);
        account.setAvailableAmount(newAvailableAmount);
        account.setUpdateTime(new Date());
        account.setUpdateName(accountDto.getRealName());
        accountService.updateById(account);

        // 收支流水：发货保证金解冻（冻结金额 -> 可用余额，属资金形态变化，不计入收支科目）
        IncomeExpense unfrozenFlow = new IncomeExpense()
                .setDirection(IncomeExpenseDirectionEnum.UNFROZEN.byteValue())
                .setDirectionDesc(IncomeExpenseDirectionEnum.UNFROZEN.getName())
                .setBizType(IncomeExpenseBizTypeEnum.SHIP_MONEY.byteValue())
                .setBizTypeName(IncomeExpenseBizTypeEnum.SHIP_MONEY.getName())
                .setIncomeExpenseType(IncomeExpenseTypeEnum.NOT_APPLICABLE.byteValue())
                .setIncomeExpenseTypeName(IncomeExpenseTypeEnum.NOT_APPLICABLE.getName())
                .setAmount(releaseAmount)
                .setOrderId(frozenDetail.getOrderNo())
                .setBizNo(frozenDetail.getFrozenNo())
                .setRemark("发货保证金解冻")
                .setCreateName(accountDto.getRealName());
        fillAfterSnapshot(unfrozenFlow, null, accountDto, BigDecimal.ZERO,
                releaseAmount, releaseAmount.negate());
        recordIncomeExpense(accountDto, unfrozenFlow);

        log.info("释放发货保证金成功: userId={}, frozenNo={}, orderNo={}, amount={}, frozenAmount={}, availableAmount={}",
                request.getUserId(), frozenDetail.getFrozenNo(), frozenDetail.getOrderNo(),
                releaseAmount, newFrozenAmount, newAvailableAmount);

        return Result.success();
    }

    @Override
    public Result<AccountDto> selectAccount(String userId) {
        return accountService.selectAccount(userId);
    }
}
