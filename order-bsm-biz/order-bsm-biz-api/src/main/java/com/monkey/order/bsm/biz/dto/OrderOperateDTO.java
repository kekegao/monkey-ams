package com.monkey.order.bsm.biz.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderOperateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 运单号（tf_b_order.order_id）
     */
    private String orderId;
}
