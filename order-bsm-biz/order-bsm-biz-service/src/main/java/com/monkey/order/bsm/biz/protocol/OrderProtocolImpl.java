package com.monkey.order.bsm.biz.protocol;

import com.monkey.order.bsm.biz.annotation.DistributedLock;
import com.monkey.order.bsm.biz.entity.Order;
import com.monkey.order.bsm.biz.service.inf.OrderService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Map;


@Slf4j
@DubboService(version = "1.0.0", group = "dev", timeout = 5000)
public class OrderProtocolImpl implements OrderProtocol {

    @Autowired
    private OrderService orderService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    @DistributedLock(key = "'order:create:' + #param['orderId']", waitTime = 3, leaseTime = -1)
    public void insertOrder(Map<String, Object> param) {

        // 模拟业务耗时
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Order order = new Order();
        order.setOrderId((String)param.get("orderId"));
        order.setShipperUserId((String)param.get("shipperUserId"));
        order.setShipperName("天宫");
        order.setShipperMobile("1896536545");
        order.setCarrierUserId("54652245553");
        order.setCarrierName("朱雀一号");
        order.setCarrierMobile("1896985245");
        order.setCreateTime(new Date());
        RBucket<String> bucket =
                redissonClient.getBucket("test:key");

        bucket.set("hello redis");
        order.setShipperName(bucket.get());
        orderService.save(order);

    }
}
