package com.monkey.account.bsm.biz.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 运费托管结算（对账）请求：将货主托管的运费扣划，并按清算单金额入帐承运方。
 * <p>
 * 资金安全：
 * 1) 扣划金额以货主冻结明细实际金额为准，调用方仅传入 frozenNo 与 orderNo 用于定位；
 * 2) 入账金额以 settlement 模块计算后的 carrierSettleAmount 为准，防止篡改；
 * 3) 整个操作在分布式锁 + 本地事务内完成，保证「货主冻结扣减」与「承运方余额增加」原子一致。
 *
 * @author gkk
 * @since 2026-09-14
 */
@Data
public class SettleMoneyAccountRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 货主用户ID（运费托管方）
     */
    private String shipperUserId;

    /**
     * 承运方用户ID（运费入账方）
     */
    private String carrierUserId;

    /**
     * 运单号
     */
    private String orderNo;

    /**
     * 运费托管冻结流水号（结算时按此流水号扣划）
     */
    private String frozenNo;

    /**
     * 已托管冻结运费金额（从货主冻结中扣划）
     */
    private BigDecimal frozenAmount;

    /**
     * 货主额外承担的平台服务费（须从货主可用余额中扣划；默认 0）
     */
    private BigDecimal shipperServiceFee;

    /**
     * 承运方实收金额（已扣除承运方服务费等，入账承运方可用余额）
     */
    private BigDecimal carrierSettleAmount;

    /**
     * 操作人（对账/结算执行人）
     */
    private String operator;
}
