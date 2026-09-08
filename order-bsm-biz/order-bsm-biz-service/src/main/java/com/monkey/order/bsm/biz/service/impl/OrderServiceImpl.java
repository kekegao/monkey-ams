package com.monkey.order.bsm.biz.service.impl;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.monkey.ams.common.auth.context.UserContext;
import com.monkey.ams.common.auth.model.LoginSession;
import com.monkey.ams.common.response.Result;
import com.monkey.ams.common.utils.StringGenerateUtil;
import com.monkey.order.bsm.biz.dto.AcceptOrderDTO;
import com.monkey.order.bsm.biz.dto.OrderDto;
import com.monkey.order.bsm.biz.dto.OrderOperateDTO;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.dto.OrderQueryDTO;
import com.monkey.order.bsm.biz.entity.Order;
import com.monkey.order.bsm.biz.mapper.OrderMapper;
import com.monkey.order.bsm.biz.service.inf.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

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
    public List<OrderDto> queryPublishOrderList(OrderQueryDTO queryDTO) {
        if (queryDTO == null || queryDTO.getShipperUserId() == null || queryDTO.getShipperUserId().trim().isEmpty()) {
            log.warn("查询已发布订单列表缺少货主用户ID");
            return java.util.Collections.emptyList();
        }
        return baseMapper.selectPublishOrderList(queryDTO);
    }

    @Override
    public List<OrderDto> querySourceOrderList(OrderQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new OrderQueryDTO();
        }
        return baseMapper.selectSourceOrderList(queryDTO);
    }

    /**
     * 承运端摘单（抢单）
     * <p>
     * 安全与并发策略：
     * 1) 承运人身份一律取自登录态 UserContext，杜绝请求参数伪造；
     * 2) 禁止货主摘取自己发布的货源（防自摘套利）；
     * 3) 状态 CAS（where id=? and status=1）单条 UPDATE 原子流转，行锁层面天然防并发；
     *    外层另有 OrderProtocol 的分布式锁做前置串行，双保险；
     * 4) 同一承运人重复摘单/重试时幂等成功，避免重复处理；
     * 5) 更新走主键索引，无 SELECT ... FOR UPDATE 长事务，抢单场景性能高。
     */
    @Override
    public Result acceptOrder(AcceptOrderDTO acceptOrderDTO) {
        // 1. 参数校验
        if (acceptOrderDTO == null || acceptOrderDTO.getOrderId() == null || acceptOrderDTO.getOrderId().trim().isEmpty()) {
            return Result.fail("请选择要摘单的货源");
        }
        // 2. 登录态校验（承运人身份以登录用户为准）
        LoginSession session = UserContext.get();
        if (session == null || session.getUserId() == null || session.getUserId().trim().isEmpty()) {
            log.warn("摘单失败：未获取到登录用户信息");
            return Result.fail("登录状态已失效，请重新登录");
        }
        String carrierUserId = session.getUserId();
        String orderId = acceptOrderDTO.getOrderId().trim();

        // 3. 定位订单（先取主键，后续 CAS 走主键索引）
        Order order = baseMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderId, orderId)
                .eq(Order::getDeleteFlag, (byte) 0)
                .last("limit 1"));
        if (order == null) {
            return Result.fail("货源不存在或已下架");
        }

        // 4. 安全校验：禁止摘取自己发布的货源
        if (carrierUserId.equals(order.getShipperUserId())) {
            log.warn("摘单失败：禁止摘取自己发布的货源, orderId={}", orderId);
            return Result.fail("不能摘取自己发布的货源");
        }

        // 5. 幂等：已是本人摘单的单，重复请求直接返回成功（防前端重试/双击误报）
        if (isAcceptedBy(order, carrierUserId)) {
            log.info("重复摘单请求，幂等返回成功: orderId={}, carrierUserId={}", orderId, carrierUserId);
            return Result.success();
        }
        // 6. 非发布态且非本人已摘，给出明确失败原因
        if (order.getStatus() == null || order.getStatus() != 1) {
            return Result.fail(order.getStatus() != null && order.getStatus() == 2
                    ? "该货源已被其他承运方摘单" : "货源当前状态不可摘单");
        }

        // 7. CAS 原子抢占：仅当仍为「发布(1)」才可摘，单条 UPDATE 行锁保证并发下只成功一人
        LambdaUpdateWrapper<Order> wrapper = Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, 2)
                .set(Order::getStatusDesc, "摘单")
                .set(Order::getCarrierUserId, carrierUserId)
                .set(Order::getCarrierUserName, session.getUserName())
                .set(Order::getCarrierMobile, session.getMobile())
                .set(Order::getUpdateTime, new Date())
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, 1);
        int rows = baseMapper.update(null, wrapper);
        if (rows == 1) {
            log.info("摘单成功: orderId={}, carrierUserId={}", orderId, carrierUserId);
            return Result.success();
        }

        // 8. CAS 竞争失败：补偿确认是否本人并发重试已抢先成功
        Order latest = baseMapper.selectById(order.getId());
        if (isAcceptedBy(latest, carrierUserId)) {
            log.info("摘单并发重试命中本人，幂等返回成功: orderId={}", orderId);
            return Result.success();
        }
        return Result.fail("手慢了，货源刚被其他承运方摘走");
    }

    /**
     * 判断订单是否已被指定承运人摘单
     */
    private boolean isAcceptedBy(Order order, String carrierUserId) {
        return order != null
                && order.getStatus() != null && order.getStatus() == 2
                && carrierUserId.equals(order.getCarrierUserId());
    }

    /**
     * 货主确认成交（状态机：摘单(2) -> 成交(3)）
     * <p>
     * 业务与安全规则：
     * 1) 仅该运单货主本人可操作，身份取自登录态 UserContext，不信任请求参数；
     * 2) 成交前必须有承运方（已摘单），保证资金/履约有明确对象；
     * 3) CAS(where id=? and status=2) 单条 UPDATE 原子流转，外层另有分布式锁，防并发互相覆盖；
     * 4) 幂等：已成交(3) 的运单重复请求直接返回成功，防止前端双击/重试误报。
     * 注：运费仍处于「托管冻结」，成交不触资金，待运单完成/取消时统一解冻结算。
     */
    @Override
    public Result dealOrder(OrderOperateDTO orderOperateDTO) {
        String orderId = normalizeOperateOrderId(orderOperateDTO);
        if (orderId == null) {
            return Result.fail("请选择要成交的运单");
        }
        LoginSession session = UserContext.get();
        if (session == null || session.getUserId() == null || session.getUserId().trim().isEmpty()) {
            return Result.fail("登录状态已失效，请重新登录");
        }
        Order order = fetchOrder(orderId);
        if (order == null) {
            return Result.fail("运单不存在或已删除");
        }
        // 安全校验：仅货主本人
        if (!session.getUserId().equals(order.getShipperUserId())) {
            log.warn("成交失败：非货主本人操作, orderId={}, operator={}", orderId, session.getUserId());
            return Result.fail("只能操作自己发布的运单");
        }
        // 幂等：已成交
        if (order.getStatus() != null && order.getStatus() == 3) {
            log.info("重复成交请求，幂等返回成功: orderId={}", orderId);
            return Result.success();
        }
        if (order.getStatus() == null || order.getStatus() != 2) {
            return Result.fail(order.getStatus() != null && order.getStatus() == 1
                    ? "该运单尚未被摘单，暂无法成交"
                    : "该运单已进入履约/结算流程，无法成交");
        }
        if (order.getCarrierUserId() == null || order.getCarrierUserId().trim().isEmpty()) {
            return Result.fail("该运单缺少承运方信息，无法成交");
        }

        // CAS 原子流转：仅当仍处于「摘单(2)」才可成交
        int rows = baseMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, 3)
                .set(Order::getStatusDesc, "成交")
                .set(Order::getUpdateTime, new Date())
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, 2));
        if (rows == 1) {
            log.info("确认成交成功: orderId={}, carrierUserId={}", orderId, order.getCarrierUserId());
            return Result.success();
        }
        // CAS 竞争失败：补偿确认是否已成交成功（并发重放场景）
        Order latest = baseMapper.selectById(order.getId());
        if (latest != null && latest.getStatus() != null && latest.getStatus() == 3) {
            log.info("成交并发重试命中已成交，幂等返回成功: orderId={}", orderId);
            return Result.success();
        }
        return Result.fail("操作失败，运单状态已变化，请刷新后重试");
    }

    /**
     * 货主取消承运方摘单（状态机：摘单(2) -> 发布(1)）
     * <p>
     * 取消成功后清空承运方信息，运单回到货源大厅重新等待其他司机摘单；
     * 运费托管冻结保持不变，不影响下次摘单履约。
     */
    @Override
    public Result cancelAccept(OrderOperateDTO orderOperateDTO) {
        String orderId = normalizeOperateOrderId(orderOperateDTO);
        if (orderId == null) {
            return Result.fail("请选择要取消摘单的运单");
        }
        LoginSession session = UserContext.get();
        if (session == null || session.getUserId() == null || session.getUserId().trim().isEmpty()) {
            return Result.fail("登录状态已失效，请重新登录");
        }
        Order order = fetchOrder(orderId);
        if (order == null) {
            return Result.fail("运单不存在或已删除");
        }
        // 安全校验：仅货主本人
        if (!session.getUserId().equals(order.getShipperUserId())) {
            log.warn("取消摘单失败：非货主本人操作, orderId={}, operator={}", orderId, session.getUserId());
            return Result.fail("只能操作自己发布的运单");
        }
        // 幂等：已是发布态且未绑定承运方，说明摘单已取消成功
        if (order.getStatus() != null && order.getStatus() == 1
                && (order.getCarrierUserId() == null || order.getCarrierUserId().trim().isEmpty())) {
            log.info("重复取消摘单请求，幂等返回成功: orderId={}", orderId);
            return Result.success();
        }
        if (order.getStatus() == null || order.getStatus() != 2) {
            return Result.fail(order.getStatus() != null && order.getStatus() == 3
                    ? "该运单已成交进入履约，无法取消摘单"
                    : "该运单当前状态不可取消摘单");
        }

        // CAS 原子流转：仅当仍处于「摘单(2)」才可取消，并清空承运方信息
        int rows = baseMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, 1)
                .set(Order::getStatusDesc, "发布")
                .set(Order::getCarrierUserId, null)
                .set(Order::getCarrierUserName, null)
                .set(Order::getCarrierMobile, null)
                .set(Order::getUpdateTime, new Date())
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, 2));
        if (rows == 1) {
            log.info("取消摘单成功，运单恢复发布: orderId={}", orderId);
            return Result.success();
        }
        // CAS 竞争失败：补偿确认是否已取消成功（并发重放场景）
        Order latest = baseMapper.selectById(order.getId());
        if (latest != null && latest.getStatus() != null && latest.getStatus() == 1
                && (latest.getCarrierUserId() == null || latest.getCarrierUserId().trim().isEmpty())) {
            return Result.success();
        }
        return Result.fail("操作失败，运单状态已变化，请刷新后重试");
    }

    /**
     * 校验并规整货主操作参数中的运单号
     */
    private String normalizeOperateOrderId(OrderOperateDTO orderOperateDTO) {
        if (orderOperateDTO == null || orderOperateDTO.getOrderId() == null) {
            return null;
        }
        String orderId = orderOperateDTO.getOrderId().trim();
        return orderId.isEmpty() ? null : orderId;
    }

    /**
     * 按运单号定位未删除的订单（先取主键，后续 CAS 走主键索引）
     */
    private Order fetchOrder(String orderId) {
        return baseMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderId, orderId)
                .eq(Order::getDeleteFlag, (byte) 0)
                .last("limit 1"));
    }

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
        order.setCreateTime(new Date());
        order.setShipperUserName(UserContext.get().getUserName());
        order.setShipperUserId(UserContext.get().getUserId());
        order.setStatus(1);
        order.setStatusDesc("发布");
        return order;
    }
}
