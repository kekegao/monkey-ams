package com.monkey.order.bsm.biz.protocol;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.account.bsm.biz.request.FrozenMoneyAccountRequest;
import com.monkey.ams.common.auth.context.UserContext;
import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.SnowflakeIdWorker;
import com.monkey.ams.common.utils.StringGenerateUtil;
import com.monkey.common.lock.annotation.DistributedLock;
import com.monkey.common.mq.core.RabbitMqProducer;
import com.monkey.order.bsm.biz.dto.AcceptOrderDTO;
import com.monkey.order.bsm.biz.dto.OrderDto;
import com.monkey.order.bsm.biz.dto.OrderOperateDTO;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.dto.OrderQueryDTO;
import com.monkey.order.bsm.biz.entity.Order;
import com.monkey.order.bsm.biz.service.inf.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
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
     * @param orderPublishDTO
     * @return
     */
    @DistributedLock(key = "'order:publish:' + #orderPublishDTO.shipperUserId", waitTime = 3, leaseTime = -1)
    @Override
    public Result publishOrder(OrderPublishDTO orderPublishDTO) {
        String orderId = StringGenerateUtil.generateOrderNo();
        //冻结运费
        String shipperUserId = orderPublishDTO.getShipperUserId();
        BigDecimal transportMoney = orderPublishDTO.getTransportMoney();
        FrozenMoneyAccountRequest request = new FrozenMoneyAccountRequest();
        request.setUserId(shipperUserId);
        request.setAmount(transportMoney);
        request.setBizType(1);
        request.setFrozenNo(StringGenerateUtil.generateOrderNo("DJ"));
        request.setOrderNo(orderId);
        Result result = accountProtocol.frozenTransportMoneyAccount(request);
        if(!result.isSuccess()) {
            log.warn("冻结运费失败: userId={}, amount={}, reason={}", shipperUserId, transportMoney, result.getMessage());
            return result;
        }

        log.info("开始发布货源");
        //发布货源
        orderPublishDTO.setOrderId(orderId);
        Result orderResult = orderService.publishOrder(orderPublishDTO);
        if(orderResult.isSuccess()){
            log.info("货源发布成功");
            return Result.success();
        }

        //如果发布失败，发送mq，回滚释放
        JSONObject data = new JSONObject();
        data.put("userId", shipperUserId);
        data.put("amount", transportMoney);
        data.put("frozenNo", request.getFrozenNo());
        rabbitMqProducer.send(ROUTING_KEY, data);
        return Result.fail();
    }

    /**
     * 查询货主已发布的订单列表
     *
     * @param orderQueryDTO 查询条件
     * @return 订单列表，按发布时间倒序
     */
    @Override
    public Result<List<OrderDto>> queryPublishOrderList(OrderQueryDTO orderQueryDTO) {
        try {
            if (orderQueryDTO == null) {
                orderQueryDTO = new OrderQueryDTO();
            }
            // ams-app 已从登录态注入货主ID，此处兜底取 dubbo 透传的登录用户
            if (orderQueryDTO.getShipperUserId() == null || orderQueryDTO.getShipperUserId().trim().isEmpty()) {
                try {
                    orderQueryDTO.setShipperUserId(UserContext.getUserId());
                } catch (Exception ignore) {
                    // 忽略，交给下方统一校验
                }
            }
            if (orderQueryDTO.getShipperUserId() == null || orderQueryDTO.getShipperUserId().trim().isEmpty()) {
                return Result.fail("登录用户信息缺失，无法查询订单列表");
            }
            List<OrderDto> orderList = orderService.queryPublishOrderList(orderQueryDTO);
            return Result.success(orderList);
        } catch (Exception e) {
            log.error("查询货主已发布订单列表失败, query={}", orderQueryDTO, e);
            return Result.fail("查询订单列表失败");
        }
    }

    /**
     * 承运端货源大厅列表（可摘货源 / 线路搜索）
     *
     * @param orderQueryDTO 查询条件：发货地/收货地关键字选填
     * @return 货源订单列表，按发布时间倒序
     */
    @Override
    public Result<List<OrderDto>> querySourceOrderList(OrderQueryDTO orderQueryDTO) {
        try {
            if (orderQueryDTO == null) {
                orderQueryDTO = new OrderQueryDTO();
            }
            List<OrderDto> sourceList = orderService.querySourceOrderList(orderQueryDTO);
            return Result.success(sourceList);
        } catch (Exception e) {
            log.error("查询货源大厅列表失败, query={}", orderQueryDTO, e);
            return Result.fail("查询货源列表失败");
        }
    }

    /**
     * 承运端「我的运单」列表：查询当前承运方所有已摘的运单
     * <p>
     * 承运方身份从 dubbo 透传的登录态 UserContext 兜底注入，避免请求参数伪造。
     *
     * @param orderQueryDTO 查询条件（status/statusList 选填）
     * @return 已摘运单列表，按发布时间倒序
     */
    @Override
    public Result<List<OrderDto>> queryCarrierOrderList(OrderQueryDTO orderQueryDTO) {
        try {
            if (orderQueryDTO == null) {
                orderQueryDTO = new OrderQueryDTO();
            }
            // ams-app 已从登录态注入承运方ID，此处兜底取 dubbo 透传的登录用户
            if (orderQueryDTO.getCarrierUserId() == null || orderQueryDTO.getCarrierUserId().trim().isEmpty()) {
                try {
                    orderQueryDTO.setCarrierUserId(UserContext.getUserId());
                } catch (Exception ignore) {
                    // 忽略，交给下方统一校验
                }
            }
            if (orderQueryDTO.getCarrierUserId() == null || orderQueryDTO.getCarrierUserId().trim().isEmpty()) {
                return Result.fail("登录用户信息缺失，无法查询运单列表");
            }
            List<OrderDto> orderList = orderService.queryCarrierOrderList(orderQueryDTO);
            return Result.success(orderList);
        } catch (Exception e) {
            log.error("查询承运方已摘运单列表失败, query={}", orderQueryDTO, e);
            return Result.fail("查询运单列表失败");
        }
    }

    /**
     * 承运端摘单（抢单）
     * <p>
     * 前置 Redisson 分布式锁按运单号串行化抢单，锁内再由 Service 层做
     * 「状态 CAS + 幂等」校验落库，双重防并发，保证同一货源只被一个承运方抢到。
     *
     * @param acceptOrderDTO 摘单参数（orderId 运单号）
     * @return 摘单结果
     */
    @DistributedLock(key = "'order:accept:' + #acceptOrderDTO.orderId", waitTime = 3, leaseTime = -1)
    @Override
    public Result acceptOrder(AcceptOrderDTO acceptOrderDTO) {
        try {
            return orderService.acceptOrder(acceptOrderDTO);
        } catch (Exception e) {
            log.error("摘单失败, acceptOrderDTO={}", acceptOrderDTO, e);
            return Result.fail("摘单失败，请稍后重试");
        }
    }

    /**
     * 货主确认成交（摘单 -> 成交）
     * <p>
     * 按运单号加分布式锁，与「取消摘单」互斥串行，配合 Service 层状态 CAS 防并发覆盖。
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 成交结果
     */
    @DistributedLock(key = "'order:deal:' + #orderOperateDTO.orderId", waitTime = 3, leaseTime = -1)
    @Override
    public Result dealOrder(OrderOperateDTO orderOperateDTO) {
        try {
            return orderService.dealOrder(orderOperateDTO);
        } catch (Exception e) {
            log.error("成交失败, orderOperateDTO={}", orderOperateDTO, e);
            return Result.fail("成交失败，请稍后重试");
        }
    }

    /**
     * 货主取消承运方摘单（摘单 -> 发布，恢复等待摘单）
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 取消摘单结果
     */
    @DistributedLock(key = "'order:cancelAccept:' + #orderOperateDTO.orderId", waitTime = 3, leaseTime = -1)
    @Override
    public Result cancelAccept(OrderOperateDTO orderOperateDTO) {
        try {
            return orderService.cancelAccept(orderOperateDTO);
        } catch (Exception e) {
            log.error("取消摘单失败, orderOperateDTO={}", orderOperateDTO, e);
            return Result.fail("取消摘单失败，请稍后重试");
        }
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
