package com.monkey.order.bsm.biz.dto;

import lombok.Data;

/**
 * 货主已发布订单列表查询条件
 */
@Data
public class OrderQueryDTO {

    /**
     * 货主用户ID（必传，一般由 ams-app 层从登录态注入）
     */
    private String shipperUserId;

    /**
     * 订单状态（选填），对应 tf_b_order.status：
     * 1发布-2摘单-3成交-4发货-5确认收货-6回单确认-7结算申请-8结算-9对账-10发票
     */
    private Integer status;
}
