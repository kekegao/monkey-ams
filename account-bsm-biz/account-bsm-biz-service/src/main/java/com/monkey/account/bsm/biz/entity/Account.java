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
import java.util.Date;

/**
 * <p>
 * 账户表
 * </p>
 *
 * @author gkk
 * @since 2026-08-26
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("tf_b_account")
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户userId
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户名称
     */
    @TableField("user_name")
    private String userName;

    /**
     * 真实名称
     */
    @TableField("real_name")
    private String realName;

    /**
     * 手机号
     */
    @TableField("mobile")
    private String mobile;

    /**
     * 余额
     */
    @TableField("balance")
    private BigDecimal balance;

    /**
     * 冻结金额
     */
    @TableField("frozen_amount")
    private BigDecimal frozenAmount;

    /**
     * 可用金额
     */
    @TableField("available_amount")
    private BigDecimal availableAmount;

    /**
     * 状态
     */
    @TableField("status")
    private Integer status;

    /**
     * 删除标记：0正常，1已删除
     */
    @TableField("delete_flag")
    private Byte deleteFlag;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 创建人
     */
    @TableField("create_name")
    private String createName;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 更新人
     */
    @TableField("update_name")
    private String updateName;
}
