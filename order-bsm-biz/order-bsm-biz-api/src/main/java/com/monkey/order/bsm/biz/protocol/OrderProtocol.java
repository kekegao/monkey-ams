package com.monkey.order.bsm.biz.protocol;


import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.AcceptOrderDTO;
import com.monkey.order.bsm.biz.dto.OrderDto;
import com.monkey.order.bsm.biz.dto.OrderOperateDTO;
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

    /**
     * 承运端货源大厅列表（可摘货源 / 线路搜索）
     *
     * @param orderQueryDTO 查询条件：shipperKeyword / carrierKeyword 选填
     * @return 货源订单列表，按发布时间倒序
     */
    Result<List<OrderDto>> querySourceOrderList(OrderQueryDTO orderQueryDTO);

    /**
     * 货主确认成交（摘单(2) -> 成交(3)）
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 成交结果
     */
    Result dealOrder(OrderOperateDTO orderOperateDTO);

    /**
     * 货主取消承运方摘单（摘单(2) -> 发布(1)，恢复等待摘单）
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 取消摘单结果
     */
    Result cancelAccept(OrderOperateDTO orderOperateDTO);

    void insertOrder(Map<String,Object> param);
}
