package com.monkey.ams.common.constants;

public class AmsRabbitConstants {

    /**
     * 路由 ROUTING_KEY：运费托管释放
     */
    public static final String ROUTING_KEY = "business.account.unfrozen";

    /**
     * 路由 ROUTING_KEY：发货保证金释放
     */
    public static final String SHIP_MONEY_ROUTING_KEY = "business.account.unfrozen.ship";
}
