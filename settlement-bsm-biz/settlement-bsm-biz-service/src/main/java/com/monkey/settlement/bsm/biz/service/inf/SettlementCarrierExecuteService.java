package com.monkey.settlement.bsm.biz.service.inf;

import com.monkey.ams.common.response.Result;

public interface SettlementCarrierExecuteService {

    /**
     * 承运方清算
     *
     * @param settlementNo
     * @param orderId
     * @return
     */
    Result executeSettlementCarrier(String settlementNo, String orderId);
}
