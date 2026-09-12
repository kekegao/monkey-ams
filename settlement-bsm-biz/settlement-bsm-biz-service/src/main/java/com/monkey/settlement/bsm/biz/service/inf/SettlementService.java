package com.monkey.settlement.bsm.biz.service.inf;

import com.monkey.settlement.bsm.biz.dto.SettlementApplyResultDto;
import com.monkey.settlement.bsm.biz.dto.SettlementCarrierDto;
import com.monkey.settlement.bsm.biz.dto.SettlementShipperDto;
import com.monkey.settlement.bsm.biz.request.SettlementApplyRequest;

/**
 * 清算单业务服务
 * <p>
 * 承载结算申请建单的编排逻辑（幂等、金额计算、双单落库），供协议层调用。
 *
 * @author gkk
 * @since 2026-09-12
 */
public interface SettlementService {

    /**
     * 结算申请：为同一运单生成（或补齐）货主清算单与承运方清算单
     *
     * @param request 结算申请参数
     * @return 两张清算单的单号与金额信息
     */
    SettlementApplyResultDto applySettlement(SettlementApplyRequest request);

    /**
     * 按运单号查询货主清算单
     *
     * @param orderId 运单号
     * @return 货主清算单，不存在返回 null
     */
    SettlementShipperDto queryShipperSettlement(String orderId);

    /**
     * 按运单号查询承运方清算单
     *
     * @param orderId 运单号
     * @return 承运方清算单，不存在返回 null
     */
    SettlementCarrierDto queryCarrierSettlement(String orderId);
}
