package com.monkey.order.bsm.biz.service.inf;


import com.baomidou.mybatisplus.spring.service.IService;
import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.AcceptOrderDTO;
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

    /**
     * 承运端货源大厅列表（可摘货源 / 线路搜索）
     *
     * @param queryDTO 查询条件：发货地/收货地关键字选填
     * @return 货源订单列表
     */
    List<OrderDto> querySourceOrderList(OrderQueryDTO queryDTO);

    /**
     * 承运端摘单（抢单）：
     * 将订单状态由 发布(1) 原子流转为 摘单(2)，并绑定承运人身份。
     * 安全要求：承运人身份取自登录态 UserContext，不信任请求参数；
     * 防并发要求：外层分布式锁 + 库内状态 CAS 双重保障同一货源只被一个承运方抢到；
     * 性能要求：更新走主键 + 单条 UPDATE，无 select-for-update 长事务。
     *
     * @param acceptOrderDTO 摘单参数（orderId 运单号）
     * @return 摘单结果
     */
    Result acceptOrder(AcceptOrderDTO acceptOrderDTO);

}
