package com.monkey.account.bsm.biz.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户收支流水 DTO（与 tf_b_income_expense 字段对齐，用于 dubbo 协议出参）
 */
@Data
public class IncomeExpenseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 收支流水号 */
    private String flowNo;

    /** 账户所属用户ID */
    private String userId;

    /** 用户名称 */
    private String userName;

    /** 账户类型：1用户账户 2平台公司对公账户 */
    private Byte accountType;

    /** 记账方向：1收入 2支出 3冻结 4解冻 */
    private Byte direction;

    /** 方向描述 */
    private String directionDesc;

    /** 业务类型：1充值 2提现 3运费托管 4发货保证金 5货主清算扣划 6承运方清算划账 7人工调账 */
    private Byte bizType;

    /** 业务类型名称 */
    private String bizTypeName;

    /** 收支科目：0不适用 1运费收入 2运费支出 3平台服务费 4充值 5提现 6保证金 7人工调账 8其他 */
    private Byte incomeExpenseType;

    /** 收支科目名称 */
    private String incomeExpenseTypeName;

    /** 发生金额（元，恒为正数） */
    private BigDecimal amount;

    /** 变动前账户余额 */
    private BigDecimal balanceBefore;

    /** 变动后账户余额 */
    private BigDecimal balanceAfter;

    /** 变动前可用余额 */
    private BigDecimal availableBefore;

    /** 变动后可用余额 */
    private BigDecimal availableAfter;

    /** 变动前冻结金额 */
    private BigDecimal frozenBefore;

    /** 变动后冻结金额 */
    private BigDecimal frozenAfter;

    /** 对手方用户ID */
    private String counterpartyUserId;

    /** 对手方名称 */
    private String counterpartyName;

    /** 对手方账户类型 */
    private Byte counterpartyAccountType;

    /** 关联运单号 */
    private String orderId;

    /** 关联业务单号 */
    private String bizNo;

    /** 资金发生时间 */
    private LocalDateTime flowTime;

    /** 状态：1成功 2处理中 3失败 4已冲正 */
    private Byte status;

    /** 状态描述 */
    private String statusDesc;

    /** 关联原流水号 */
    private String relatedFlowNo;

    /** 备注 */
    private String remark;

    /** 删除标记：0正常 1已删除 */
    private Byte deleteFlag;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建人 */
    private String createName;

    /** 更新人 */
    private String updateName;
}
