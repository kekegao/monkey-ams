package com.monkey.order.bsm.biz.protocol;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.SnowflakeIdWorker;
import com.monkey.common.lock.annotation.DistributedLock;
import com.monkey.common.mq.core.RabbitMqProducer;
import com.monkey.order.bsm.biz.entity.Order;
import com.monkey.order.bsm.biz.service.inf.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import static com.monkey.ams.common.constants.AmsRabbitConstants.ROUTING_KEY;


@Slf4j
@DubboService
public class OrderProtocolImpl implements OrderProtocol {

    @DubboReference
    private AccountProtocol accountProtocol;

    @Autowired
    private OrderService orderService;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private SnowflakeIdWorker idService;

    @Resource
    private RabbitMqProducer rabbitMqProducer;


    /**
     * 发布货源
     *
     * @param param
     * @return
     */
    @DistributedLock(key = "'order:publish:' + #param['shipperUserId']", waitTime = 3, leaseTime = -1)
    @Override
    public Result publishOrder(Map<String, Object> param) {

        //冻结运费
        String transportMoney = MapUtils.getString(param, "transportMoney");
        String shipperUserId = MapUtils.getString(param, "shipperUserId");
        Result result = accountProtocol.frozenTransportMoneyAccount(shipperUserId,new BigDecimal(transportMoney));
        if(!result.isSuccess()) {
            log.info("**********************************************");
            return result;
        }

        log.info("开始发布货源");
        //发布货源
        Result orderResult = orderService.publishOrder(param);
        if(orderResult.isSuccess()){
            log.info("货源发布成功");
            return Result.success();
        }

        //如果发布失败，发送mq，回滚释放
        JSONObject data = new JSONObject();
        data.put("userId",shipperUserId);
        data.put("amount",transportMoney);
        rabbitMqProducer.send(ROUTING_KEY,data);
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
