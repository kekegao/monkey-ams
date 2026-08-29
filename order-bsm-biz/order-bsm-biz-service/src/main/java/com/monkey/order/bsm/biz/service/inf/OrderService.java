package com.monkey.order.bsm.biz.service.inf;


import com.baomidou.mybatisplus.spring.service.IService;
import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.entity.Order;

import java.util.Map;

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
     * @param param
     * @return
     */
    Result publishOrder(Map<String, Object> param);

}
