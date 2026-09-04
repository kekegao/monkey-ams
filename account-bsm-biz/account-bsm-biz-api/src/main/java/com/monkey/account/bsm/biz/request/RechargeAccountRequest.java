package com.monkey.account.bsm.biz.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RechargeAccountRequest implements Serializable {

    private String userId;
    /**
     * 充值金额
     */
    private BigDecimal amount;
}
