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
 * 货主结算单（应付清算单）表
 * </p>
 *
 * @author gkk
 * @since 2026-09-12
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("tf_b_settlement_shipper")
public class SettlementShipper implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 货主结算单号（HJS + 23位流水）
     */
    @TableField("settlement_no")
    private String settlementNo;

    /**
     * 运单号（tf_b_order.order_id）
     */
    @TableField("order_id")
    private String orderId;

    /**
     * 货主用户ID（tf_b_order.shipper_user_id）
     */
    @TableField("shipper_user_id")
    private String shipperUserId;

    /**
     * 货主用户名称（冗余，便于直接展示）
     */
    @TableField("shipper_user_name")
    private String shipperUserName;

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
     * 承运方用户ID（冗余，便于双方核对同一运单）
     */
    @TableField("carrier_user_id")
    private String carrierUserId;

    /**
     * 承运方名称（冗余）
     */
    @TableField("carrier_name")
    private String carrierName;

    /**
     * 承运方手机号（冗余）
     */
    @TableField("carrier_mobile")
    private String carrierMobile;

    /**
     * 订单运费金额（tf_b_order.transport_money）
     */
    @TableField("transport_money")
    private BigDecimal transportMoney;

    /**
     * 货主承担的服务费率（如 0.0500 表示 5%），默认 0
     */
    @TableField("service_fee_rate")
    private BigDecimal serviceFeeRate;

    /**
     * 货主承担的平台服务费
     */
    @TableField("service_fee")
    private BigDecimal serviceFee;

    /**
     * 优惠/减免金额
     */
    @TableField("discount_amount")
    private BigDecimal discountAmount;

    /**
     * 其他费用（正数加收、负数减免）
     */
    @TableField("other_amount")
    private BigDecimal otherAmount;

    /**
     * 应付总额=运费+服务费-优惠+其他
     */
    @TableField("payable_amount")
    private BigDecimal payableAmount;

    /**
     * 托管冻结流水号（tf_b_frozen_detail.frozen_no，便于追溯扣划来源）
     */
    @TableField("frozen_no")
    private String frozenNo;

    /**
     * 已托管冻结金额（对应运费托管明细合计）
     */
    @TableField("frozen_amount")
    private BigDecimal frozenAmount;

    /**
     * 实付金额（结算(8)完成后由托管运费中扣划）
     */
    @TableField("paid_amount")
    private BigDecimal paidAmount;

    /**
     * 退还货主金额=托管金额-实付金额
     */
    @TableField("refund_amount")
    private BigDecimal refundAmount;

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
     * 结算申请时间
     */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /**
     * 结算完成（扣划）时间
     */
    @TableField("settle_time")
    private LocalDateTime settleTime;

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
