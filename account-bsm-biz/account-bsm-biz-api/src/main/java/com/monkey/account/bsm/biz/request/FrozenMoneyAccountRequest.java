package com.monkey.account.bsm.biz.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class FrozenMoneyAccountRequest implements Serializable {

    private String userId;

    private BigDecimal amount;

    /**
     * 冻结业务类型：1运费托管 2承运方发货保证金 3提现冻结
     */
    private Integer bizType;

    /**
     * 冻结流水号
     */
    private String frozenNo;
    /**
     * 关联单号（YD运单号/TX提现单号）
     */
    private String orderNo;
}
