package com.monkey.settlement.bsm.biz.api;

import com.monkey.ams.common.response.Result;
import com.monkey.settlement.bsm.biz.dto.SettlementApplyResultDto;
import com.monkey.settlement.bsm.biz.dto.SettlementCarrierDto;
import com.monkey.settlement.bsm.biz.dto.SettlementShipperDto;
import com.monkey.settlement.bsm.biz.request.SettlementApplyRequest;

/**
 * 清算（结算单）服务协议
 * <p>
 * 职责：运单「结算申请(7)」时按运单生成「货主清算单（应付）」与「承运方清算单（应收）」，
 * 并对外提供按运单号查询两侧清算单的能力，供订单、对账、结算等场景使用。
 * <p>
 * 一致性约定：
 * 1) 两张单在清算服务同一本地事务内落库，避免"只有货主单没有承运方单"的中间态；
 * 2) 以运单号（orderId）为幂等键，表上 uk_order_id 唯一索引兜底，重复调用只返回已存在单据；
 * 3) 金额一律由清算服务端根据运单运费与服务费率计算，不信任调用方传入的金额结果。
 *
 * @author gkk
 * @since 2026-09-12
 */
public interface SettlementProtocol {

    /**
     * 结算申请：生成（或补齐）货主清算单与承运方清算单。
     * <p>
     * 幂等：同一运单号重复调用不会重复建单，返回已存在单据信息（existed=true）。
     *
     * @param request 结算申请参数（运单号、货主/承运方信息、运费等，均以运单快照为准）
     * @return 两张清算单的单号及金额信息
     */
    Result<SettlementApplyResultDto> applySettlement(SettlementApplyRequest request);

    /**
     * 按运单号查询货主清算单（对账/前端详情用）
     *
     * @param orderId 运单号
     * @return 货主清算单
     */
    Result<SettlementShipperDto> queryShipperSettlement(String orderId);

    /**
     * 按运单号查询承运方清算单（对账/前端详情用）
     *
     * @param orderId 运单号
     * @return 承运方清算单
     */
    Result<SettlementCarrierDto> queryCarrierSettlement(String orderId);
}
