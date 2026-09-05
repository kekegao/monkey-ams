package com.monkey.order.bsm.biz.service.inf;


import com.baomidou.mybatisplus.spring.service.IService;
import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.OrderDto;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.dto.OrderQueryDTO;
import com.monkey.order.bsm.biz.entity.Order;

import java.util.List;

/**
 * <p>
 * 订单表 服务类
 * </p>
 *
 * @author gkk
 * @since 2026-08-17
 */
public interface OrderService extends IService<Order> {

    /**
     * 发布运单
     * @param orderPublishDTO 发布运单请求参数
     * @return
     */
    Result publishOrder(OrderPublishDTO orderPublishDTO);

    /**
     * 查询货主已发布的订单列表（「我的订单」列表页）
     *
     * @param queryDTO 查询条件
     * @return 订单列表
     */
    List<OrderDto> queryPublishOrderList(OrderQueryDTO queryDTO);

}
