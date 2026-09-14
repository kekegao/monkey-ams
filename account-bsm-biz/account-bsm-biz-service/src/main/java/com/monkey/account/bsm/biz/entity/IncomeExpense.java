package com.monkey.account.bsm.biz.entity;

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
 * 账户收入支出流水表
 * </p>
 *
 * @author gkk
 * @since 2026-09-14
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("tf_b_income_expense")
public class IncomeExpense implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 收支流水号（SZ+yyyyMMddHHmmssSSS+6位随机）
     */
    @TableField("flow_no")
    private String flowNo;

    /**
     * 账户所属用户ID（tf_b_account.user_id）
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户名称（冗余，便于直接展示）
     */
    @TableField("user_name")
    private String userName;

    /**
     * 账户类型：1用户账户 2平台公司对公账户（account.platform.company-user-id 配置的账户）
     */
    @TableField("account_type")
    private Byte accountType;

    /**
     * 记账方向：1收入 2支出 3冻结 4解冻
     */
    @TableField("direction")
    private Byte direction;

    /**
     * 方向描述（冗余）：收入/支出/冻结/解冻
     */
    @TableField("direction_desc")
    private String directionDesc;

    /**
     * 业务类型：1充值 2提现 3运费托管 4发货保证金 5货主清算扣划 6承运方清算划账 7人工调账
     */
    @TableField("biz_type")
    private Byte bizType;

    /**
     * 业务类型名称（冗余）：充值/提现/运费托管/发货保证金/货主清算扣划/承运方清算划账/人工调账
     */
    @TableField("biz_type_name")
    private String bizTypeName;

    /**
     * 收支科目：0不适用(冻结/解冻) 1运费收入 2运费支出 3平台服务费 4充值 5提现 6保证金 7人工调账 8其他
     */
    @TableField("income_expense_type")
    private Byte incomeExpenseType;

    /**
     * 收支科目名称（冗余）：运费收入/运费支出/平台服务费/充值/提现/保证金/人工调账/其他
     */
    @TableField("income_expense_type_name")
    private String incomeExpenseTypeName;

    /**
     * 发生金额（元，恒为正数；方向由 direction 表达）
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 变动前账户余额（tf_b_account.balance）
     */
    @TableField("balance_before")
    private BigDecimal balanceBefore;

    /**
     * 变动后账户余额
     */
    @TableField("balance_after")
    private BigDecimal balanceAfter;

    /**
     * 变动前可用余额（tf_b_account.available_amount）
     */
    @TableField("available_before")
    private BigDecimal availableBefore;

    /**
     * 变动后可用余额
     */
    @TableField("available_after")
    private BigDecimal availableAfter;

    /**
     * 变动前冻结金额（tf_b_account.frozen_amount）
     */
    @TableField("frozen_before")
    private BigDecimal frozenBefore;

    /**
     * 变动后冻结金额
     */
    @TableField("frozen_after")
    private BigDecimal frozenAfter;

    /**
     * 对手方用户ID（跨账户划转时必填：货主清算填平台公司、承运方划账填平台公司）
     */
    @TableField("counterparty_user_id")
    private String counterpartyUserId;

    /**
     * 对手方名称（冗余）
     */
    @TableField("counterparty_name")
    private String counterpartyName;

    /**
     * 对手方账户类型：1用户账户 2平台公司对公账户
     */
    @TableField("counterparty_account_type")
    private Byte counterpartyAccountType;

    /**
     * 关联运单号（tf_b_order.order_id，非运单类业务可为空）
     */
    @TableField("order_id")
    private String orderId;

    /**
     * 关联业务单号（清算单号/提现单号/冻结流水号/充值单号），幂等去重用
     */
    @TableField("biz_no")
    private String bizNo;

    /**
     * 资金发生时间
     */
    @TableField("flow_time")
    private LocalDateTime flowTime;

    /**
     * 状态：1成功(已入账) 2处理中 3失败 4已冲正
     */
    @TableField("status")
    private Byte status;

    /**
     * 状态描述（冗余）：成功/处理中/失败/已冲正
     */
    @TableField("status_desc")
    private String statusDesc;

    /**
     * 关联原流水号（冲正/反向记账时回填，指向被冲正的流水）
     */
    @TableField("related_flow_no")
    private String relatedFlowNo;

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
