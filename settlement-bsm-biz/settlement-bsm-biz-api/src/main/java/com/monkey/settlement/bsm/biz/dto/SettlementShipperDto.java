package com.monkey.settlement.bsm.biz.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 货主清算单（应付清算单）传输对象
 *
 * @author gkk
 * @since 2026-09-12
 */
@Data
public class SettlementShipperDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 货主结算单号（HJS 前缀）
     */
    private String settlementNo;

    /**
     * 运单号（tf_b_order.order_id）
     */
    private String orderId;

    /**
     * 货主用户ID（tf_b_order.shipper_user_id）
     */
    private String shipperUserId;

    /**
     * 货主用户名称（冗余，便于直接展示）
     */
    private String shipperUserName;

    /**
     * 货主名称/企业名称（冗余）
     */
    private String shipperName;

    /**
     * 货主手机号（冗余）
     */
    private String shipperMobile;

    /**
     * 承运方用户ID（冗余，便于双方核对同一运单）
     */
    private String carrierUserId;

    /**
     * 承运方名称（冗余）
     */
    private String carrierName;

    /**
     * 订单运费金额（tf_b_order.transport_money）
     */
    private BigDecimal transportMoney;

    /**
     * 货主承担的服务费率（如 0.0500 表示 5%）
     */
    private BigDecimal serviceFeeRate;

    /**
     * 货主承担的平台服务费
     */
    private BigDecimal serviceFee;

    /**
     * 优惠/减免金额
     */
    private BigDecimal discountAmount;

    /**
     * 其他费用（正数加收、负数减免）
     */
    private BigDecimal otherAmount;

    /**
     * 应付总额=运费+服务费-优惠+其他
     */
    private BigDecimal payableAmount;

    /**
     * 托管冻结流水号（tf_b_frozen_detail.frozen_no）
     */
    private String frozenNo;

    /**
     * 已托管冻结金额（对应运费托管明细合计）
     */
    private BigDecimal frozenAmount;

    /**
     * 实付金额（结算(8)完成后由托管运费中扣划）
     */
    private BigDecimal paidAmount;

    /**
     * 退还货主金额=托管金额-实付金额
     */
    private BigDecimal refundAmount;

    /**
     * 结算批次号（平台批量结算时写入，可为空）
     */
    private String settleBatchNo;

    /**
     * 结算状态：1待结算 2结算中 3已结算 4已作废
     */
    private Byte status;

    /**
     * 状态描述（冗余）
     */
    private String statusDesc;

    /**
     * 开票状态：0未开票 1已开票
     */
    private Byte invoiceStatus;

    /**
     * 结算申请时间
     */
    private Date applyTime;

    /**
     * 结算完成（扣划）时间
     */
    private Date settleTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标记：0正常 1已删除
     */
    private Byte deleteFlag;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createName;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 更新人
     */
    private String updateName;
}
