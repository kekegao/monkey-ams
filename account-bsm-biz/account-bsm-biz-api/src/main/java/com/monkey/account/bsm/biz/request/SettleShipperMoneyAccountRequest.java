package com.monkey.account.bsm.biz.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 货主清算请求：将货主托管的冻结运费扣划，并结算至平台公司对公账户。
 * <p>
 * 业务边界：只做货主侧资金动作，不做承运方任何入账（承运方结算由独立流程处理）。
 * <p>
 * 资金安全：
 * 1) 平台公司对公账户由账户服务配置指定，调用方不可传入，避免资金流向被篡改；
 * 2) 扣划金额以账户侧冻结明细实际金额为准（资金唯一真相源），本请求的 frozenAmount 仅用于比对告警；
 * 3) 按冻结流水号（缺省时按 货主+运单号+运费托管）定位「冻结中」明细，明细已打款时幂等返回成功；
 * 4) 整个操作在分布式锁 + 本地事务内完成，保证「货主冻结/余额扣减 + 冻结明细置已打款 + 平台对公账户入账」原子一致。
 *
 * @author gkk
 * @since 2026-09-14
 */
@Data
public class SettleShipperMoneyAccountRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 货主用户ID（运费托管方，即清算归属方）
     */
    private String shipperUserId;

    /**
     * 运单号
     */
    private String orderNo;

    /**
     * 运费托管冻结流水号（清算时按此流水号扣划；可为空，为空时按 货主+运单号+运费托管 定位）
     */
    private String frozenNo;

    /**
     * 已托管冻结运费金额（清算单快照，仅用于与账户侧实际冻结额比对，不作为扣划依据）
     */
    private BigDecimal frozenAmount;

    /**
     * 货主承担的平台服务费（须从货主可用余额中扣划，与冻结运费一并计入平台对公账户；默认 0）
     */
    private BigDecimal shipperServiceFee;

    /**
     * 操作人（清算执行人）
     */
    private String operator;
}
