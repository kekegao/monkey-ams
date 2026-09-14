package com.monkey.account.bsm.biz.api;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.dto.AccountDto;
import com.monkey.account.bsm.biz.request.FrozenMoneyAccountRequest;
import com.monkey.account.bsm.biz.request.SettleCarrierMoneyAccountRequest;
import com.monkey.account.bsm.biz.request.SettleMoneyAccountRequest;
import com.monkey.account.bsm.biz.request.SettleShipperMoneyAccountRequest;
import com.monkey.account.bsm.biz.request.UnFrozenMoneyAccountRequest;
import com.monkey.ams.common.response.Result;

import java.math.BigDecimal;

public interface AccountProtocol {

    /**
     * 开户
     *
     * @param jsonObject
     * @return
     */
    Result openAccount(JSONObject jsonObject);


    /**
     * 冻结运费
     *
     * @param request
     * @return
     */
    Result frozenTransportMoneyAccount(FrozenMoneyAccountRequest request);


    /**
     * 释放运费
     *
     * @param request
     * @return
     */
    Result unfrozenTransportMoneyAccount(UnFrozenMoneyAccountRequest request);


    /**
     * 冻结承运方保证金
     *
     * @param request
     * @return
     */
    Result frozenCarrierMoneyAccount(FrozenMoneyAccountRequest request);

    /**
     * 释放承运方保证金
     *
     * @param request
     * @return
     */
    Result unFrozenCarrierMoneyAccount(UnFrozenMoneyAccountRequest request);

    /**
     * 运费托管结算（对账扣划）：货主托管运费 -> 承运方账户
     * <p>
     * 按冻结流水号将货主处于冻结中的运费托管明细扣划（状态置为已打款），
     * 同时将清算单计算后的承运方实收金额增加到承运方可用余额。
     * 幂等：同一冻结流水号重复扣划时，若明细已处于已打款状态，直接返回成功。
     *
     * @param request 结算请求（须包含 shipperUserId、carrierUserId、orderNo、frozenNo、carrierSettleAmount）
     * @return 结算结果
     * @deprecated 承运方对账已改为「平台公司对公账户 -> 承运方账户」划账（货主托管运费在货主清算阶段
     * 已扣划至平台对公账户，对账时不再从货主侧扣减），请使用
     * {@link #settleCarrierMoneyFromCompanyAccount(SettleCarrierMoneyAccountRequest)}
     */
    @Deprecated
    Result settleTransportMoneyAccount(SettleMoneyAccountRequest request);

    /**
     * 承运方清算（对账）划账：平台公司对公账户 -> 承运方账户
     * <p>
     * 从平台公司对公账户（账户服务配置指定，调用方不可传入）出账，划付至承运方智运宝账户可用余额。
     * 业务边界：只做平台对承运方的划付，不做货主侧任何扣减（货主托管运费已在货主清算阶段扣划至平台对公账户）。
     * 幂等：以承运方清算单号为业务锚点落地划账流水，同一清算单重复请求时直接返回已划金额，绝不重复出账。
     *
     * @param request 划账请求（须包含 carrierUserId、settlementNo、orderNo、settleAmount）
     * @return 实际划付至承运方账户的金额（元）
     */
    Result<BigDecimal> settleCarrierMoneyFromCompanyAccount(SettleCarrierMoneyAccountRequest request);

    /**
     * 货主清算：货主托管运费 -> 平台公司对公账户
     * <p>
     * 按冻结流水号将货主处于冻结中的运费托管明细扣划（状态置为已打款），
     * 同时将「实际扣划冻结额 + 货主服务费」结算至平台公司对公账户（账户由服务端配置指定，调用方不可传入）。
     * 幂等：同一冻结流水号重复扣划时，若明细已处于已打款状态，直接返回成功。
     * 业务边界：只做货主侧资金动作，不做承运方任何入账（承运方结算由独立流程处理）。
     *
     * @param request 清算请求（须包含 shipperUserId、orderNo，frozenNo 可为空）
     * @return 实际从托管冻结中扣划的金额（元）
     */
    Result<BigDecimal> settleShipperMoneyToCompanyAccount(SettleShipperMoneyAccountRequest request);

    /**
     * 查询智运宝账户
     *
     * @param userId
     * @return
     */
    Result<AccountDto> selectAccount(String userId);

}
