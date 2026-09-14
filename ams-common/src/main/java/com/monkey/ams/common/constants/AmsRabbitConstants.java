package com.monkey.ams.common.constants;

public class AmsRabbitConstants {

    /**
     * 路由 ROUTING_KEY：运费托管释放
     */
    public static final String ROUTING_KEY = "business.account.unfrozen.routing.key";

    /**
     * 路由 ROUTING_KEY：发货保证金释放
     */
    public static final String SHIP_MONEY_ROUTING_KEY = "business.account.unfrozen.ship.routing.key";

    /**
     * 路由 ROUTING_KEY：结算延迟key
     */
    public static final String SETTLEMENT_DELAY_ROUTING_KEY = "monkey.settlement.delay.routing.key";


    public static final String SETTLEMENT_EXECUTE_ROUTING_KEY = "monkey.settlement.execute.routing.key";
}
