package com.monkey.account.bsm.biz.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 账户冻结明细记录 DTO（tf_b_frozen_detail）
 * </p>
 *
 * @author gkk
 * @since 2026-09-07
 */
@Data
public class FrozenDetailDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 账户所属用户ID（tf_b_account.user_id）
     */
    private String userId;

    /**
     * 用户名称（冗余，便于直接展示）
     */
    private String userName;

    /**
     * 冻结流水号
     */
    private String frozenNo;

    /**
     * 冻结业务类型：1运费托管 2提现冻结
     */
    private Byte bizType;

    /**
     * 业务类型名称（冗余）：运费托管/提现冻结
     */
    private String bizTypeName;


    /**
     * 关联单号（YD运单号/TX提现单号）
     */
    private String orderNo;

    /**
     * 冻结金额（元），须大于0
     */
    private BigDecimal amount;

    /**
     * 冻结时间
     */
    private Date frozenTime;

    /**
     * 状态：1冻结中/处理中 2已解冻(退回可用余额) 3已打款(提现完成)
     */
    private Byte status;

    /**
     * 状态描述（冗余）：冻结中/已解冻/已打款
     */
    private String statusDesc;

    /**
     * 结束时间：运费托管解冻时间 或 提现打款时间
     */
    private Date finishTime;

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
