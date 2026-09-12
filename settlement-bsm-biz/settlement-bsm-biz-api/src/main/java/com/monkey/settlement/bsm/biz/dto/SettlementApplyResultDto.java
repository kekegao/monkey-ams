package com.monkey.settlement.bsm.biz.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 结算申请建单结果
 * <p>
 * 返回同一运单生成的「货主清算单」与「承运方清算单」关键信息，便于调用方日志留痕与前端展示。
 *
 * @author gkk
 * @since 2026-09-12
 */
@Data
public class SettlementApplyResultDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 货主清算单主键
     */
    private Long shipperSettlementId;

    /**
     * 货主清算单号（HJS 前缀）
     */
    private String shipperSettlementNo;

    /**
     * 货主应付总额
     */
    private BigDecimal payableAmount;

    /**
     * 货主已托管冻结金额
     */
    private BigDecimal frozenAmount;

    /**
     * 承运方清算单主键
     */
    private Long carrierSettlementId;

    /**
     * 承运方清算单号（CJS 前缀）
     */
    private String carrierSettlementNo;

    /**
     * 承运方实收金额（后续「结算(8)」打款依据）
     */
    private BigDecimal settleAmount;

    /**
     * 承运方承担的平台服务费
     */
    private BigDecimal carrierServiceFee;

    /**
     * 是否命中幂等：true 表示该运单清算单此前已生成，本次未重复创建
     */
    private Boolean existed;
}
