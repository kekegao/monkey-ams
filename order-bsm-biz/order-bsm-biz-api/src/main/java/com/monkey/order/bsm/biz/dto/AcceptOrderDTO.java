package com.monkey.order.bsm.biz.dto;

import lombok.Data;

import java.io.Serializable;


/**
 * 摘单请求参数
 *
 * @author gkk
 */
@Data
public class AcceptOrderDTO  implements Serializable {

    /**
     * 运单号（tf_b_order.order_id）
     */
    private String orderId;
}
