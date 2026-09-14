package com.monkey.settlement.bsm.biz.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.monkey.ams.common.constants.AmsRabbitConstants.SETTLEMENT_DELAY_ROUTING_KEY;
import static com.monkey.ams.common.constants.AmsRabbitConstants.SETTLEMENT_EXECUTE_ROUTING_KEY;
import static com.monkey.common.mq.constants.RabbitConstants.*;
import static com.monkey.settlement.bsm.biz.constants.SettlementMqConstants.SETTLEMENT_DELAY_QUEUE;
import static com.monkey.settlement.bsm.biz.constants.SettlementMqConstants.SETTLEMENT_EXECUTE_QUEUE;

@Configuration
public class SettlementRabbitMqConfig {

    /**
     * 延迟 Exchange
     */
    @Bean
    public DirectExchange settlementDelayExchange() {

        return new DirectExchange(
                SETTLEMENT_DELAY_EXCHANGE,
                true,
                false
        );
    }


    /**
     * 延迟 Queue
     *
     * 消息进入这个 Queue 后，
     * 到达 TTL 后自动变成死信。
     */
    @Bean
    public Queue settlementDelayQueue() {

        Map<String, Object> arguments = new HashMap<>();

        // 默认 1 分钟
        arguments.put(
                "x-message-ttl",
                60 * 1000
        );

        // 死信 Exchange
        arguments.put(
                "x-dead-letter-exchange",
                SETTLEMENT_DLX_EXCHANGE
        );

        // 死信 RoutingKey
        arguments.put(
                "x-dead-letter-routing-key",
                SETTLEMENT_EXECUTE_ROUTING_KEY
        );

        return new Queue(
                SETTLEMENT_DELAY_QUEUE,
                true,
                false,
                false,
                arguments
        );
    }

    /**
     * 延迟 Exchange -> 延迟 Queue
     */
    @Bean
    public Binding settlementDelayBinding() {

        return BindingBuilder
                .bind(settlementDelayQueue())
                .to(settlementDelayExchange())
                .with(SETTLEMENT_DELAY_ROUTING_KEY);
    }



    /**
     * 死信 Exchange
     */
    @Bean
    public DirectExchange settlementDlxExchange() {

        return new DirectExchange(
                SETTLEMENT_DLX_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 真正执行结算的 Queue
     */
    @Bean
    public Queue settlementExecuteQueue() {

        return new Queue(
                SETTLEMENT_EXECUTE_QUEUE,
                true
        );
    }

    /**
     * DLX -> Execute Queue  绑定
     */
    @Bean
    public Binding settlementExecuteBinding() {

        return BindingBuilder
                .bind(settlementExecuteQueue())
                .to(settlementDlxExchange())
                .with(SETTLEMENT_EXECUTE_ROUTING_KEY);
    }


}
