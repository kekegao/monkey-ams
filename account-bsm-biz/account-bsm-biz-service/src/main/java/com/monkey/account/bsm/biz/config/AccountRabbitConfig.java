package com.monkey.account.bsm.biz.config;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.monkey.account.bsm.biz.constants.AccountRabbitConstants.UNFROZEN_SHIP_MONEY_ACCOUNT_QUEUE;
import static com.monkey.account.bsm.biz.constants.AccountRabbitConstants.UNFROZEN_TRANSPORT_MONEY_ACCOUNT_QUEUE;
import static com.monkey.ams.common.constants.AmsRabbitConstants.ROUTING_KEY;
import static com.monkey.ams.common.constants.AmsRabbitConstants.SHIP_MONEY_ROUTING_KEY;
import static com.monkey.common.mq.constants.RabbitConstants.BUSINESS_EXCHANGE;

@Configuration
public class AccountRabbitConfig {

    @Bean
    public DirectExchange businessExchange() {
        return new DirectExchange(BUSINESS_EXCHANGE, true, false);
    }


    @Bean
    public Queue unfrozenTransportMoneyQueue() {
        return QueueBuilder
                .durable(UNFROZEN_TRANSPORT_MONEY_ACCOUNT_QUEUE)
                .build();
    }


    @Bean
    public Binding orderCreatedBinding(
            Queue unfrozenTransportMoneyQueue,
            DirectExchange businessExchange) {

        return BindingBuilder
                .bind(unfrozenTransportMoneyQueue)
                .to(businessExchange)
                .with(ROUTING_KEY);
    }


    /**
     * 发货保证金释放队列
     */
    @Bean
    public Queue unfrozenShipMoneyQueue() {
        return QueueBuilder
                .durable(UNFROZEN_SHIP_MONEY_ACCOUNT_QUEUE)
                .build();
    }


    /**
     * 发货保证金释放队列绑定
     */
    @Bean
    public Binding unfrozenShipMoneyBinding(
            Queue unfrozenShipMoneyQueue,
            DirectExchange businessExchange) {

        return BindingBuilder
                .bind(unfrozenShipMoneyQueue)
                .to(businessExchange)
                .with(SHIP_MONEY_ROUTING_KEY);
    }
}
