package com.monkey.order.bsm.biz.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.StringGenerateUtil;
import com.monkey.order.bsm.biz.entity.Order;
import com.monkey.order.bsm.biz.mapper.OrderMapper;
import com.monkey.order.bsm.biz.service.inf.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

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

    //@DistributedLock(key = "'order:create:' + #order.orderId", waitTime = 3, leaseTime = -1)
    @Override
    public Result publishOrder(Map<String, Object> param) {
        //生成运单号
        try {
            String orderId = StringGenerateUtil.generateOrderNo();
            Order order = new Order();
            order.setOrderId(orderId);
            order.setCreateTime(new Date());
            order.setShipperUserId(param.get("shipperUserId").toString());
            order.setShipperUserName(param.get("shipperUserName").toString());
            order.setShipperName(param.get("shipperName").toString());
            order.setShipperMobile(param.get("shipperMobile").toString());
            order.setShipperAddress(param.get("shipperAddress").toString());
            order.setShipperProvince(param.get("shipperProvince").toString());
            order.setShipperCity(param.get("shipperCity").toString());
            order.setShipperArea(param.get("shipperArea").toString());
            order.setCarrierProvince(param.get("carrierProvince").toString());
            order.setCarrierCity(param.get("carrierCity").toString());
            order.setCarrierArea(param.get("carrierArea").toString());
            order.setCarrierAddress(param.get("carrierAddress").toString());
            order.setGoodsType(param.get("goodsType").toString());
            order.setGoodsDescription(param.get("goodsDescription").toString());
            order.setGoodsWeight(new BigDecimal(param.get("goodsWeight").toString()));
            order.setStatus(1);
            if(this.save(order)){
                return Result.success();
            }
            return Result.fail();
        }catch (Exception e){
            log.error(e.getMessage());
            return Result.fail();
        }

    }
}
