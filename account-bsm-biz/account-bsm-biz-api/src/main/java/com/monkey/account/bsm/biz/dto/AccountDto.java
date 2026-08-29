package com.monkey.account.bsm.biz.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class AccountDto implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户userId
     */
    private String userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 真实名称
     */
    private String realName;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 余额
     */
    private BigDecimal balance;

    /**
     * 冻结金额
     */
    private BigDecimal frozenAmount;

    /**
     * 可用金额
     */
    private BigDecimal availableAmount;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 删除标记：0正常，1已删除
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
