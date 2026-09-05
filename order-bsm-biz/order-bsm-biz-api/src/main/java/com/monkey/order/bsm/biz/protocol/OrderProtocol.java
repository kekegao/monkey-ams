package com.monkey.order.bsm.biz.protocol;


import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.AcceptOrderDTO;
import com.monkey.order.bsm.biz.dto.OrderDto;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.dto.OrderQueryDTO;

import java.util.List;
import java.util.Map;

public interface OrderProtocol {

    /**
     * 发布运单
     *
     * @param orderPublishDTO 发布运单请求参数
     * @return
     */
    Result publishOrder(OrderPublishDTO orderPublishDTO);

    /**
     * 摘单
     *
     * @param acceptOrderDTO
     * @return
     */
    Result acceptOrder(AcceptOrderDTO acceptOrderDTO);

    /**
     * 查询货主已发布的订单列表（「我的订单」列表页）
     *
     * @param orderQueryDTO 查询条件：shipperUserId 必传，status 选填
     * @return 订单列表，按发布时间倒序
     */
    Result<List<OrderDto>> queryPublishOrderList(OrderQueryDTO orderQueryDTO);

    void insertOrder(Map<String,Object> param);
}
