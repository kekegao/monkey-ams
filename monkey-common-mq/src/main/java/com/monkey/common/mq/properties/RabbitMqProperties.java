package com.monkey.common.mq.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "monkey.rabbitmq")
public class RabbitMqProperties {

    /**
     * 是否启用 RabbitMQ Starter
     */
    private boolean enabled = true;

    /**
     * 消息最大重试次数
     */
    private int maxAttempts = 3;

    /**
     * 重试初始间隔，单位毫秒
     */
    private long initialInterval = 1000;

    /**
     * 重试最大间隔，单位毫秒
     */
    private long maxInterval = 10000;

    /**
     * 重试间隔倍数
     */
    private double multiplier = 2.0;
}
