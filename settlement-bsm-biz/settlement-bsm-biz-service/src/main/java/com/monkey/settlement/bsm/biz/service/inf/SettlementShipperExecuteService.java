package com.monkey.settlement.bsm.biz.service.inf;

import com.monkey.ams.common.response.Result;

/**
 * 货主清算执行（MQ 消费端）
 *
 * @author gkk
 * @since 2026-09-14
 */
public interface SettlementShipperExecuteService {

    /**
     * 货主清算执行
     * <p>
     * 业务边界：只做货主自身清算——从货主托管冻结金额扣减，并结算至平台公司对公账户；
     * 不做任何承运方入账与记账（承运方结算由 {@link SettlementCarrierExecuteService} 单独处理）。
     * <p>
     * 幂等设计（四重防护）：
     * 1) 落库幂等：先落地 tf_b_settlement_shipper_work_order 工单，由 uk_settlement_no / uk_idempotent_key
     *    兜底，重复消息不会产生第二条工单；
     * 2) 状态幂等：工单已「处理成功」直接返回成功，不重复发起资金动作；
     * 3) CAS 抢占：工单状态由「已投递/待投递/处理失败」CAS 至「处理中」，抢占失败即判定为并发重复消费，直接跳过；
     * 4) 资金幂等：清算单状态「已结算」短路返回；账户侧按冻结流水号幂等，异常重放不会重复扣款。
     * <p>
     * 并发/高可用：方法级分布式锁（按清算单号串行）+ 工单/清算单 DB 状态机（多实例安全）+
     * 结算单「待结算→结算中→已结算」两阶段推进，保证同一条运单只清算一次。
     *
     * @param settlementNo 货主清算单号（tf_b_settlement_shipper.settlement_no）
     * @param orderId      运单号（tf_b_order.order_id）
     * @param messageId    MQ 消息ID（工单幂等与链路追溯，可为空，为空时按运单号生成幂等键）
     * @param payload      消息体快照（JSON，落工单便于排障与重放，可为空）
     * @return 执行结果；失败时 message 为可直接返回前端的提示
     */
    Result executeSettlementShipper(String settlementNo, String orderId, String messageId, String payload);
}
