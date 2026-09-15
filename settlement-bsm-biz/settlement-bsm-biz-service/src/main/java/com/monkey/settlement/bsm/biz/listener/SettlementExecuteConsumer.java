package com.monkey.settlement.bsm.biz.listener;

import com.alibaba.fastjson.JSONObject;
import com.monkey.ams.common.constants.UserTypeEnum;
import com.monkey.ams.common.response.Result;
import com.monkey.common.mq.model.RabbitMessage;
import com.monkey.settlement.bsm.biz.service.inf.SettlementCarrierExecuteService;
import com.monkey.settlement.bsm.biz.service.inf.SettlementShipperExecuteService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.monkey.settlement.bsm.biz.constants.SettlementMqConstants.SETTLEMENT_EXECUTE_QUEUE;

/**
 * 清算执行消费者（货主清算 / 承运方清算）
 * <p>
 * 消费可靠性设计：
 * 1) 手动 ACK：业务成功才 basicAck；
 * 2) 业务失败：登记失败事实（清算工单 status=5 + next_retry_time，由重试调度补偿重投）后 basicNack 且不重回队列，
 *    避免消息在队列头部热循环打爆下游；
 * 3) 消息/用户类型非法：直接 ACK 丢弃，不占用队列；
 * 4) 消费幂等由各自的 ExecuteService 内部保证（工单落库 + 状态机 CAS + 资金侧幂等）。
 *
 * @author gkk
 * @since 2026-09-14
 */
@Slf4j
@Component
public class SettlementExecuteConsumer {

    @Autowired
    private SettlementCarrierExecuteService settlementCarrierExecuteService;

    @Autowired
    private SettlementShipperExecuteService settlementShipperExecuteService;

    @RabbitListener(
            queues = SETTLEMENT_EXECUTE_QUEUE
    )
    public void consume(
            RabbitMessage<JSONObject> message,
            Channel channel,
            Message rabbitMessage) throws IOException {

        long deliveryTag = rabbitMessage.getMessageProperties().getDeliveryTag();
        String messageId = message == null ? null : message.getMessageId();

        try {
            // ==========================
            // 消息解析
            // ==========================
            JSONObject data = message == null ? null : message.getData();
            if (data == null) {
                log.warn("收到空的清算消息，直接丢弃: messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            String settlementNo = data.getString("settlementNo");
            String orderId = data.getString("orderId");
            Integer userType = data.getInteger("userType");
            log.info("收到清算消息: messageId={}, settlementNo={}, orderId={}, userType={}",
                    messageId, settlementNo, orderId, userType);

            Result result;
            if (userType != null && userType == UserTypeEnum.SHIPPER.getValue()) {
                // 货主清算：只做货主侧清算（冻结扣划 -> 平台公司对公账户），承运方结算单独处理
                result = settlementShipperExecuteService.executeSettlementShipper(
                        settlementNo, orderId, messageId, data.toJSONString());
                if (result == null || !result.isSuccess()) {
                    log.error("货主清算失败: messageId={}, settlementNo={}, orderId={}, msg={}",
                            messageId, settlementNo, orderId, result == null ? "无返回" : result.getMessage());
                }
            } else if (userType != null && userType == UserTypeEnum.DRIVER.getValue()) {
                result = settlementCarrierExecuteService.executeSettlementCarrier(settlementNo, orderId);
                if (result == null || !result.isSuccess()) {
                    log.error("承运方清算失败: messageId={}, settlementNo={}, orderId={}, msg={}",
                            messageId, settlementNo, orderId, result == null ? "无返回" : result.getMessage());
                }
            } else {
                log.warn("清算消息用户类型非法，直接丢弃: messageId={}, userType={}", messageId, userType);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // ==========================
            // 手动 ACK / NACK
            // ==========================
            if (result != null && result.isSuccess()) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 业务失败：失败事实已登记在清算工单（status=5 + next_retry_time），此处不重回队列，交由重试调度补偿
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            log.error("清算消息消费异常: messageId={}", messageId, e);
            channel.basicNack(deliveryTag, false, false);
            // 交给 RetryInterceptor
            throw e;
        }
    }
}
