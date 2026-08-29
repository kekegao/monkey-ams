package com.monkey.account.bsm.biz.config;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.monkey.account.bsm.biz.constants.AccountRabbitConstants.UNFROZEN_TRANSPORT_MONEY_ACCOUNT_QUEUE;
import static com.monkey.ams.common.constants.AmsRabbitConstants.ROUTING_KEY;
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
}
