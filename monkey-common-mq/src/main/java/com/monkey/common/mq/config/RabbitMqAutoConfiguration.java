package com.monkey.common.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monkey.common.mq.core.RabbitMqProducer;
import com.monkey.common.mq.properties.RabbitMqProperties;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import static com.monkey.common.mq.constants.RabbitConstants.BUSINESS_EXCHANGE;

@AutoConfiguration
@EnableConfigurationProperties(RabbitMqProperties.class)
@ConditionalOnProperty(
        prefix = "monkey.rabbitmq",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RabbitMqAutoConfiguration {

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(
                BUSINESS_EXCHANGE,
                true,
                false
        );
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    @ConditionalOnMissingBean
    public Jackson2JsonMessageConverter rabbitMessageConverter(
            ObjectMapper objectMapper) {

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setExchange(BUSINESS_EXCHANGE);
        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }


    /**
     * RabbitMQ 消费者监听容器
     */
    @Bean
    @ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter,
            RabbitMqProperties properties) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);

        factory.setMessageConverter(messageConverter);

        /*
         * 手动 ACK
         */
        factory.setAcknowledgeMode(
                org.springframework.amqp.core.AcknowledgeMode.MANUAL
        );

        /*
         * 消费失败后进行重试
         */
        factory.setDefaultRequeueRejected(false);

        return factory;
    }


    /**
     * RabbitMQ Producer
     */
    @Bean
    @ConditionalOnMissingBean
    public RabbitMqProducer rabbitMqProducer(
            RabbitTemplate rabbitTemplate) {

        return new RabbitMqProducer(rabbitTemplate);
    }
}
