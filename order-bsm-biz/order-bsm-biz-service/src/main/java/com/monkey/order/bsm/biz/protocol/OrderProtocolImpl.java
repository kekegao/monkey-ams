package com.monkey.order.bsm.biz.protocol;

import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.SnowflakeIdWorker;
import com.monkey.ams.common.utils.StringGenerateUtil;
import com.monkey.order.bsm.biz.annotation.DistributedLock;
import com.monkey.order.bsm.biz.entity.Order;
import com.monkey.order.bsm.biz.service.inf.OrderService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;


@Slf4j
@DubboService
public class OrderProtocolImpl implements OrderProtocol {

    @Autowired
    private OrderService orderService;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private SnowflakeIdWorker idService;


    @DistributedLock(key = "'order:publish:' + #param['shipperUserId']", waitTime = 3, leaseTime = -1)
    @Override
    public Result publishOrder(Map<String, Object> param) {

        //生成运单号
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

        if(orderService.publishOrder(order)){
            return Result.success();
        }
        return Result.fail();
    }

    @Override
    @DistributedLock(key = "'order:create:' + #param['orderId']", waitTime = 3, leaseTime = -1)
    public void insertOrder(Map<String, Object> param) {

        Order order = new Order();
        order.setOrderId((String)param.get("orderId"));
        order.setShipperUserId(idService.nextId());
        order.setShipperName("天宫");
        order.setShipperMobile("1896536545");
        order.setCarrierUserId(idService.nextId());
        order.setCarrierName("朱雀一号");
        order.setCarrierMobile("1896985245");
        order.setCreateTime(new Date());
        RBucket<String> bucket = redissonClient.getBucket(order.getShipperUserId());
        bucket.set("shipperUserId:"+order.getShipperUserId());
        bucket = redissonClient.getBucket(order.getCarrierUserId());
        bucket.set("carrierUserId:"+order.getCarrierUserId());
        orderService.save(order);

    }
}
