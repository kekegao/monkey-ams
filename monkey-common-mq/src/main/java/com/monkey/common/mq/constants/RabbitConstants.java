package com.monkey.common.mq.constants;

public class RabbitConstants {

    /**
     * 业务 Exchange
     */
    public static final String BUSINESS_EXCHANGE = "monkey.business.exchange";


    /**
     * ==============================
     * 结算延迟消息
     * ==============================
     */

    public static final String SETTLEMENT_DELAY_EXCHANGE = "monkey.settlement.delay.exchange";

    /**
     * ==============================
     * 死信 Exchange
     * ==============================
     */

    public static final String SETTLEMENT_DLX_EXCHANGE = "monkey.settlement.dlx.exchange";

}
