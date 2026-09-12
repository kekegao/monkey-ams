package com.monkey.order.bsm.biz.protocol;


import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.AcceptOrderDTO;
import com.monkey.order.bsm.biz.dto.OrderDto;
import com.monkey.order.bsm.biz.dto.OrderOperateDTO;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.dto.OrderQueryDTO;

import java.util.List;
import java.util.Map;

public interface OrderProtocol {

    /**
     * 发布运单
     *
     * @param orderPublishDTO 发布运单请求参数
     * @return
     */
    Result publishOrder(OrderPublishDTO orderPublishDTO);

    /**
     * 摘单
     *
     * @param acceptOrderDTO
     * @return
     */
    Result acceptOrder(AcceptOrderDTO acceptOrderDTO);

    /**
     * 查询货主已发布的订单列表（「我的订单」列表页）
     *
     * @param orderQueryDTO 查询条件：shipperUserId 必传，status 选填
     * @return 订单列表，按发布时间倒序
     */
    Result<List<OrderDto>> queryPublishOrderList(OrderQueryDTO orderQueryDTO);

    /**
     * 承运端货源大厅列表（可摘货源 / 线路搜索）
     *
     * @param orderQueryDTO 查询条件：shipperKeyword / carrierKeyword 选填
     * @return 货源订单列表，按发布时间倒序
     */
    Result<List<OrderDto>> querySourceOrderList(OrderQueryDTO orderQueryDTO);

    /**
     * 承运端「我的运单」列表：查询当前承运方所有已摘的运单
     *
     * @param orderQueryDTO 查询条件（carrierUserId 由登录态兜底注入，status/statusList 选填）
     * @return 已摘运单列表
     */
    Result<List<OrderDto>> queryCarrierOrderList(OrderQueryDTO orderQueryDTO);

    /**
     * 承运方确认发货（成交(3) -> 发货(4)），启动实际运输
     *
     * @param orderOperateDTO 承运方操作参数（orderId 运单号）
     * @return 发货结果
     */
    Result shipOrder(OrderOperateDTO orderOperateDTO);

    /**
     * 确认收货（发货(4) -> 确认收货(5)），确认货物已送达
     *
     * @param orderOperateDTO 承运方操作参数（orderId 运单号）
     * @return 确认收货结果
     */
    Result confirmReceipt(OrderOperateDTO orderOperateDTO);

    /**
     * 货主回单确认（确认收货(5) -> 回单确认(6)），回单确认成功后释放承运方发货保证金
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 回单确认结果
     */
    Result receiptConfirm(OrderOperateDTO orderOperateDTO);

    /**
     * 货主结算申请（回单确认(6) -> 结算申请(7)），发起托管运费结算
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 结算申请结果
     */
    Result settleApply(OrderOperateDTO orderOperateDTO);

    /**
     * 货主确认成交（摘单(2) -> 成交(3)）
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 成交结果
     */
    Result dealOrder(OrderOperateDTO orderOperateDTO);

    /**
     * 货主取消承运方摘单（摘单(2) -> 发布(1)，恢复等待摘单）
     *
     * @param orderOperateDTO 货主操作参数（orderId 运单号）
     * @return 取消摘单结果
     */
    Result cancelAccept(OrderOperateDTO orderOperateDTO);

    void insertOrder(Map<String,Object> param);
}
