package com.monkey.account.bsm.biz.listener;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.ams.common.response.Result;
import com.monkey.common.mq.model.RabbitMessage;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.monkey.account.bsm.biz.constants.AccountRabbitConstants.UNFROZEN_TRANSPORT_MONEY_ACCOUNT_QUEUE;

@Slf4j
@Component
public class AccountUnfrozenTransportMoneyConsumer {

    @Autowired
    private AccountProtocol accountProtocol;


    @RabbitListener(
            queues = UNFROZEN_TRANSPORT_MONEY_ACCOUNT_QUEUE
    )
    public void consume(
            RabbitMessage<JSONObject> message,
            Channel channel,
            Message rabbitMessage) throws IOException {

        long deliveryTag = rabbitMessage.getMessageProperties().getDeliveryTag();

        try {
            log.info("收到运费释放消息:{}",message.getMessageId());

            // ==========================
            // 业务处理
            // ==========================

            JSONObject data = message.getData();
            Result result = accountProtocol.unfrozenTransportMoneyAccount(data.getString("userId"),data.getBigDecimal("amount"));
            if(!result.isSuccess()) log.error("运费释放失败:{}", result.getMessage());

            // ==========================
            // 手动 ACK
            // ==========================

            channel.basicAck(
                    deliveryTag,
                    false
            );

        } catch (Exception e) {

            // ==========================
            // 消费失败
            // ==========================

            channel.basicNack(
                    deliveryTag,
                    false,
                    false
            );

            throw e;
        }
    }
}
