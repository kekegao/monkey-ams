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
 * 账户冻结明细记录表
 * </p>
 *
 * @author gkk
 * @since 2026-09-07
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("tf_b_frozen_detail")
public class FrozenDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
     * 冻结业务类型：1运费托管 2提现冻结
     */
    @TableField("biz_type")
    private Byte bizType;

    /**
     * 业务类型名称（冗余）：运费托管/提现冻结
     */
    @TableField("biz_type_name")
    private String bizTypeName;

    /**
     * 关联业务ID（运单ID/提现申请ID）
     */
    @TableField("ref_id")
    private String refId;

    /**
     * 关联单号（YD运单号/TX提现单号）
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 冻结金额（元），须大于0
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 冻结时间
     */
    @TableField("frozen_time")
    private LocalDateTime frozenTime;

    /**
     * 状态：1冻结中/处理中 2已解冻(退回可用余额) 3已打款(提现完成)
     */
    @TableField("status")
    private Byte status;

    /**
     * 状态描述（冗余）：冻结中/已解冻/已打款
     */
    @TableField("status_desc")
    private String statusDesc;

    /**
     * 结束时间：运费托管解冻时间 或 提现打款时间
     */
    @TableField("finish_time")
    private LocalDateTime finishTime;

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
