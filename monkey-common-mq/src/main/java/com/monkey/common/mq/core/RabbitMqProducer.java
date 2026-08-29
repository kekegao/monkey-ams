package com.monkey.common.mq.core;


import com.monkey.common.mq.model.RabbitMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

public class RabbitMqProducer {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送消息
     */
    public <T> void send(String routingKey, T data) {

        RabbitMessage<T> message = new RabbitMessage<>();

        message.setMessageId(UUID.randomUUID().toString());

        message.setData(data);

        message.setTimestamp(System.currentTimeMillis());

        //rabbitTemplate.send(message);
        rabbitTemplate.convertAndSend(routingKey,message);
    }
}
