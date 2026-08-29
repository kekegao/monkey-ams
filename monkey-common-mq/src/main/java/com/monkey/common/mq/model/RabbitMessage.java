package com.monkey.common.mq.model;


import lombok.Data;

import java.io.Serializable;

@Data
public class RabbitMessage<T> implements Serializable {

    /**
     * 消息唯一ID
     */
    private String messageId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 消息数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;
}
