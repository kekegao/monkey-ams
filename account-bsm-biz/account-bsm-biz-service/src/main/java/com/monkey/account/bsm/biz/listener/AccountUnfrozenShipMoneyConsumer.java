package com.monkey.account.bsm.biz.listener;

import com.alibaba.fastjson.JSONObject;
import com.monkey.account.bsm.biz.api.AccountProtocol;
import com.monkey.account.bsm.biz.request.UnFrozenMoneyAccountRequest;
import com.monkey.ams.common.response.Result;
import com.monkey.common.mq.model.RabbitMessage;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.monkey.account.bsm.biz.constants.AccountRabbitConstants.UNFROZEN_SHIP_MONEY_ACCOUNT_QUEUE;

/**
 * 发货保证金冻结释放监听
 * <p>
 * 消费「发货保证金释放」消息，解冻承运方的发货保证金：
 * 冻结金额扣减保证金，可用余额加回保证金，并将冻结明细置为「已解冻」。
 */
@Slf4j
@Component
public class AccountUnfrozenShipMoneyConsumer {

    @Autowired
    private AccountProtocol accountProtocol;


    @RabbitListener(
            queues = UNFROZEN_SHIP_MONEY_ACCOUNT_QUEUE
    )
    public void consume(
            RabbitMessage<JSONObject> message,
            Channel channel,
            Message rabbitMessage) throws IOException {

        long deliveryTag = rabbitMessage.getMessageProperties().getDeliveryTag();

        try {
            log.info("收到发货保证金释放消息:{}", message.getMessageId());

            // ==========================
            // 业务处理
            // ==========================

            JSONObject data = message.getData();
            UnFrozenMoneyAccountRequest request = JSONObject.parseObject(data.toJSONString(), UnFrozenMoneyAccountRequest.class);
            Result result = accountProtocol.unFrozenCarrierMoneyAccount(request);
            if (!result.isSuccess()) log.error("发货保证金释放失败:{}", result.getMessage());

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
