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
}
