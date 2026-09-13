package com.monkey.order.bsm.biz.service.inf;


import com.baomidou.mybatisplus.spring.service.IService;
import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.AcceptOrderDTO;
import com.monkey.order.bsm.biz.dto.OrderDto;
import com.monkey.order.bsm.biz.dto.OrderOperateDTO;
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

    /**
     * 承运端「我的运单」列表：查询当前承运方所有已摘的运单（status >= 2）
     *
     * @param queryDTO 查询条件：carrierUserId 必传，status/statusList 选填
     * @return 已摘运单列表，按发布时间倒序
     */
    List<OrderDto> queryCarrierOrderList(OrderQueryDTO queryDTO);

    /**
     * 承运方确认发货：将订单由 成交(3) 流转为 发货(4)，启动实际运输。
     * 安全要求：仅该运单承运方本人可操作（登录态 UserContext 校验）；
     * 防并发要求：外层分布式锁 + 状态 CAS，避免重复发货或与取消成交等并发操作互相覆盖；
     * 幂等要求：已发货(4)的运单重复请求直接返回成功。
     *
     * @param orderOperateDTO 承运方操作参数（orderId 运单号）
     * @return 发货结果
     */
    Result shipOrder(OrderOperateDTO orderOperateDTO);

    /**
     * 确认收货：将订单由 发货(4) 流转为 确认收货(5)，确认货物已送达。
     * 安全要求：仅该运单承运方本人可操作（登录态 UserContext 校验）；
     * 防并发要求：外层分布式锁 + 状态 CAS，避免重复确认或与其它流转操作互相覆盖；
     * 幂等要求：已确认收货(5)的运单重复请求直接返回成功。
     *
     * @param orderOperateDTO 承运方操作参数（orderId 运单号）
     * @return 确认收货结果
     */
    Result confirmReceipt(OrderOperateDTO orderOperateDTO);

    /**
     * 货主回单确认：将订单由 确认收货(5) 流转为 回单确认(6)，标志回单签收核对完成。
     * 安全要求：仅该运单货主本人可操作（登录态 UserContext 校验）；
     * 防并发要求：外层分布式锁 + 状态 CAS，避免重复确认；
     * 幂等要求：已回单确认(6)的运单重复请求直接返回成功；
     * 资金动作：回单确认成功后异步释放承运方的发货保证金（MQ 解耦，账户模块消费解冻）。
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 回单确认结果
     */
    Result receiptConfirm(OrderOperateDTO orderOperateDTO);

    /**
     * 货主结算申请：将订单由 回单确认(6) 流转为 结算申请(7)，发起托管运费结算。
     * 安全要求：仅该运单货主本人可操作（登录态 UserContext 校验）；
     * 防并发要求：外层分布式锁 + 状态 CAS，避免重复申请；
     * 幂等要求：已处于结算申请及之后状态的运单重复请求直接返回成功；
     * 资金说明：申请仅推进结算流程，托管运费仍冻结，实际运费支付在后续「结算(8)」阶段完成。
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 结算申请结果
     */
    Result settleApply(OrderOperateDTO orderOperateDTO);

    /**
     * 货主确认成交：将订单由 摘单(2) 流转为 成交(3)，与承运方达成正式合作。
     * 安全要求：仅该运单货主本人可操作（登录态 UserContext 校验）；
     * 防并发要求：外层分布式锁 + 状态 CAS，避免成交与取消摘单等操作互相覆盖；
     * 幂等要求：已成交(3)的运单重复请求直接返回成功。
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 成交结果
     */
    Result dealOrder(OrderOperateDTO orderOperateDTO);

    /**
     * 货主取消承运方摘单：将订单由 摘单(2) 恢复为 发布(1)，并清空承运方信息，
     * 运单回到货源大厅重新等待摘单。
     * 安全/并发/幂等要求同上。
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 取消摘单结果
     */
    Result cancelAccept(OrderOperateDTO orderOperateDTO);

}
