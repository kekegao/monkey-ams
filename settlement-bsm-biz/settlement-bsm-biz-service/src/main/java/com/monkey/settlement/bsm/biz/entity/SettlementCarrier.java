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
 * 承运方结算单（应收清算单）表
 * </p>
 *
 * @author gkk
 * @since 2026-09-12
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("tf_b_settlement_carrier")
public class SettlementCarrier implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 承运方结算单号（CJS+23位流水：yyyyMMddHHmmssSSS+6位随机）
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
     * 承运方用户ID（tf_b_order.carrier_user_id）
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
     * 承运方承担的服务费率（如 0.0500 表示 5%）
     */
    @TableField("service_fee_rate")
    private BigDecimal serviceFeeRate;

    /**
     * 承运方承担的平台服务费（佣金，从运费内扣）
     */
    @TableField("service_fee")
    private BigDecimal serviceFee;

    /**
     * 税费（如代扣税费，默认 0）
     */
    @TableField("tax_amount")
    private BigDecimal taxAmount;

    /**
     * 其他费用加项（装卸费/等待费等，正数加收）
     */
    @TableField("other_amount")
    private BigDecimal otherAmount;

    /**
     * 其他扣款（违约金/货损赔付等，正数扣减）
     */
    @TableField("deductible_amount")
    private BigDecimal deductibleAmount;

    /**
     * 实收金额=运费+其他费用-服务费-税费-扣款
     */
    @TableField("settle_amount")
    private BigDecimal settleAmount;

    /**
     * 发货保证金（回单确认时已解冻退回，仅展示核算，不参与实收计算）
     */
    @TableField("ship_deposit_amount")
    private BigDecimal shipDepositAmount;

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
     * 结算批次号（平台批量结算时写入，可为空）
     */
    @TableField("settle_batch_no")
    private String settleBatchNo;

    /**
     * 结算状态：1待结算 2结算中 3已结算 4已作废
     */
    @TableField("status")
    private Byte status;

    /**
     * 状态描述（冗余）：待结算/结算中/已结算/已作废
     */
    @TableField("status_desc")
    private String statusDesc;

    /**
     * 开票状态：0未开票 1已开票（对应订单状态机 10 发票）
     */
    @TableField("invoice_status")
    private Byte invoiceStatus;

    /**
     * 结算申请时间（与货主结算单同时生成）
     */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /**
     * 结算完成时间
     */
    @TableField("settle_time")
    private LocalDateTime settleTime;

    /**
     * 实际打款到承运方账户时间
     */
    @TableField("pay_time")
    private LocalDateTime payTime;

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
