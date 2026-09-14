package com.monkey.account.bsm.biz.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 承运方清算（对账）划账请求：平台公司对公账户 -> 承运方智运宝账户。
 * <p>
 * 业务边界：本请求只做「平台出账 + 承运方入账」，不涉及货主冻结扣减；
 * 货主托管运费在「货主清算」阶段已扣划至平台公司对公账户，本条链路只是把承运方应得款项付出去。
 * <p>
 * 资金安全：
 * 1) 出账账户（平台公司对公账户）由账户服务配置指定，调用方不可传入，避免资金被划往不确定账户；
 * 2) 出账使用 SQL 层原子扣减并带「可用余额充足」条件，杜绝并发透支出账；
 * 3) 幂等锚点为「承运方清算单号」（划账流水表的 frozen_no），同一清算单重复划账时直接返回已划金额；
 * 4) 整个操作在分布式锁 + 本地事务内完成，保证「平台出账 + 承运方入账 + 划账流水落地」原子一致。
 *
 * @author gkk
 * @since 2026-09-14
 */
@Data
public class SettleCarrierMoneyAccountRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 承运方用户ID（收款方）
     */
    private String carrierUserId;

    /**
     * 承运方清算单号（划账幂等锚点，对应落库划账流水的 frozen_no）
     */
    private String settlementNo;

    /**
     * 运单号
     */
    private String orderNo;

    /**
     * 应划付承运方金额（清算单实收金额，由 settlement 模块计算后传入，账户侧据此出账并对账）
     */
    private BigDecimal settleAmount;

    /**
     * 操作人（对账执行人）
     */
    private String operator;
}
