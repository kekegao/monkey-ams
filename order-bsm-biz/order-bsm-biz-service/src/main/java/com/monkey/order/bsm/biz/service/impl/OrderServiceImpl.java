package com.monkey.order.bsm.biz.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.monkey.ams.common.auth.context.UserContext;
import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.StringGenerateUtil;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.entity.Order;
import com.monkey.order.bsm.biz.mapper.OrderMapper;
import com.monkey.order.bsm.biz.service.inf.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * <p>
 * 订单表 服务实现类，包含发布，发货，运输等等88
 * </p>
 *
 * @author gkk
 * @since 2026-08-17
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Override
    public Result publishOrder(OrderPublishDTO orderPublishDTO) {
        try {
            Order order = buildOrder(orderPublishDTO);
            if (this.save(order)) {
                log.info("发布运单成功: orderId={}", order.getOrderId());
                return Result.success();
            }
            return Result.fail("发布运单失败");
        } catch (Exception e) {
            log.error("发布运单失败, orderPublishDTO={}", orderPublishDTO, e);
            return Result.fail("发布运单失败：" + e.getMessage());
        }
    }

    /**
     * 将发布参数DTO转为订单实体（字段名一致直接拷贝，额外字段手动赋值）
     */
    private Order buildOrder(OrderPublishDTO orderPublishDTO) {
        Order order = new Order();
        BeanUtils.copyProperties(orderPublishDTO, order);
        order.setOrderId(StringGenerateUtil.generateOrderNo());
        order.setCreateTime(new Date());
        order.setShipperUserName(UserContext.get().getUserName());
        order.setStatus(1);
        return order;
    }
}
