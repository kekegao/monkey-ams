package com.monkey.settlement.bsm.biz.constants;

public final class SettlementMqConstants {

    private SettlementMqConstants() {
    }

    /**
     * 结算延迟队列
     */
    public static final String SETTLEMENT_DELAY_QUEUE = "monkey.settlement.delay.queue";

    /**
     * 结算私信队列，真正执行的队列
     */
    public static final String SETTLEMENT_EXECUTE_QUEUE = "monkey.settlement.execute.queue";
}
