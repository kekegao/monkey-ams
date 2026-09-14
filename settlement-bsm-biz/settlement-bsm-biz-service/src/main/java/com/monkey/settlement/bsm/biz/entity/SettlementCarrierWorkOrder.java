package com.monkey.settlement.bsm.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 承运方清算MQ工单记录表
 * </p>
 *
 * @author gkk
 * @since 2026-09-14
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("tf_b_settlement_carrier_work_order")
public class SettlementCarrierWorkOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 清算工单号（CJSG+流水：yyyyMMddHHmmss+序号）
     */
    @TableField("work_order_no")
    private String workOrderNo;

    /**
     * 承运方清算单号（tf_b_settlement_carrier.settlement_no）
     */
    @TableField("settlement_no")
    private String settlementNo;

    /**
     * 运单号（tf_b_order.order_id）
     */
    @TableField("order_id")
    private String orderId;

    /**
     * 货主用户ID（冗余，便于双方核对同一运单）
     */
    @TableField("shipper_user_id")
    private String shipperUserId;

    /**
     * 货主名称/企业名称（冗余）
     */
    @TableField("shipper_name")
    private String shipperName;

    /**
     * 货主手机号（冗余）
     */
    @TableField("shipper_mobile")
    private String shipperMobile;

    /**
     * 承运方用户ID（冗余，清算归属方）
     */
    @TableField("carrier_user_id")
    private String carrierUserId;

    /**
     * 承运方用户名称（冗余，便于直接展示）
     */
    @TableField("carrier_user_name")
    private String carrierUserName;

    /**
     * 承运方名称/车队名称（冗余）
     */
    @TableField("carrier_name")
    private String carrierName;

    /**
     * 承运方手机号（冗余）
     */
    @TableField("carrier_mobile")
    private String carrierMobile;

    /**
     * 应收运费金额（tf_b_order.transport_money）
     */
    @TableField("transport_money")
    private BigDecimal transportMoney;

    /**
     * 承运方承担的平台服务费（佣金，从运费内扣）
     */
    @TableField("service_fee")
    private BigDecimal serviceFee;

    /**
     * 实收金额=运费+其他费用-服务费-税费-扣款（清算金额快照）
     */
    @TableField("settle_amount")
    private BigDecimal settleAmount;

    /**
     * 收款户名（快照，建议加密/脱敏存储）
     */
    @TableField("receiver_account_name")
    private String receiverAccountName;

    /**
     * 收款银行（快照）
     */
    @TableField("receiver_bank_name")
    private String receiverBankName;

    /**
     * 收款账号（快照，建议仅存前4后4）
     */
    @TableField("receiver_bank_card_no")
    private String receiverBankCardNo;

    /**
     * MQ 交换机（monkey.business.exchange）
     */
    @TableField("exchange_name")
    private String exchangeName;

    /**
     * MQ 路由键（business.settlement.carrier.clearing）
     */
    @TableField("routing_key")
    private String routingKey;

    /**
     * MQ 队列名（settlement_carrier_clearing_queue）
     */
    @TableField("queue_name")
    private String queueName;

    /**
     * MQ 消息唯一ID（工单创建时预生成，投递/消费统一幂等标识）
     */
    @TableField("message_id")
    private String messageId;

    /**
     * 消息类型（SETTLEMENT_CARRIER_CLEARING）
     */
    @TableField("message_type")
    private String messageType;

    /**
     * MQ 业务ID（RabbitMessage.businessId，取运单号）
     */
    @TableField("business_id")
    private String businessId;

    /**
     * 消息体快照（JSON，便于重放与排障）
     */
    @TableField("payload")
    private String payload;

    /**
     * 延迟投递秒数（走延迟队列，默认 30S）
     */
    @TableField("delay_seconds")
    private Integer delaySeconds;

    /**
     * 工单状态：1待投递 2已投递 3处理中 4处理成功 5处理失败 6已作废
     */
    @TableField("status")
    private Byte status;

    /**
     * 状态描述（冗余）：待投递/已投递/处理中/处理成功/处理失败/已作废
     */
    @TableField("status_desc")
    private String statusDesc;

    /**
     * 已重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 最大重试次数（超出后不再自动重试并告警）
     */
    @TableField("max_retry_count")
    private Integer maxRetryCount;

    /**
     * 消息投递时间（首次发送到 MQ 的时间）
     */
    @TableField("send_time")
    private LocalDateTime sendTime;

    /**
     * 消息消费时间（消费端开始处理时间）
     */
    @TableField("consume_time")
    private LocalDateTime consumeTime;

    /**
     * 处理完成时间（成功或最终失败）
     */
    @TableField("finish_time")
    private LocalDateTime finishTime;

    /**
     * 下次重试时间（重试调度扫描依据）
     */
    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    /**
     * 最近一次失败原因
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 删除标记：0正常 1已删除
     */
    @TableField("delete_flag")
    private Byte deleteFlag;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @TableField("create_name")
    private String createName;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    @TableField("update_name")
    private String updateName;
}
