package com.monkey.account.bsm.biz.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UnFrozenMoneyAccountRequest {

    private String userId;

    private BigDecimal amount;
    /**
     * 冻结流水号
     */
    private String frozenNo;

    /**
     * 关联单号（YD运单号）：未传 frozenNo 时，按 userId + orderNo + bizType 定位冻结明细
     */
    private String orderNo;

    /**
     * 冻结业务类型：1运费托管 2承运方发货保证金 3提现冻结
     */
    private Integer bizType;
}
