package com.monkey.order.bsm.biz.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 货主已发布订单列表查询条件
 */
@Data
public class OrderQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 货主用户ID（必传，一般由 ams-app 层从登录态注入）
     */
    private String shipperUserId;

    /**
     * 订单状态（选填，单值精确匹配），对应 tf_b_order.status：
     * 1发布-2摘单-3成交-4发货-5确认收货-6回单确认-7结算申请-8结算-9对账-10发票
     */
    private Integer status;

    /**
     * 订单状态列表（选填，IN 查询），用于前端「全部/待接单/已接单/运输中/已完成/已取消」等多状态归并筛选。
     * 与 status 同时存在时，以 statusList 为准。
     */
    private List<Integer> statusList;
}
