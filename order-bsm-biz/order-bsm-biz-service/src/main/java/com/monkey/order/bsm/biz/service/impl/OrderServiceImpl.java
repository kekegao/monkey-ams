package com.monkey.order.bsm.biz.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.monkey.order.bsm.biz.entity.Order;
import com.monkey.order.bsm.biz.mapper.OrderMapper;
import com.monkey.order.bsm.biz.service.inf.OrderService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author gkk
 * @since 2026-08-17
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

}
