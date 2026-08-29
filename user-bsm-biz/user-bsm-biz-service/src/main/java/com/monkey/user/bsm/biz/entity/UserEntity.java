package com.monkey.user.bsm.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户表实体
 *
 * @author gkk
 */
@Data
@TableName("tf_b_user")
public class UserEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID（雪花算法生成） */
    @TableId("user_id")
    private String userId;

    /** 用户名 */
    @TableField("user_name")
    private String userName;

    /** 用户类型 */
    @TableField("user_type")
    private Integer userType;

    /** 用户类型 */
    @TableField("user_type_desc")
    private String userTypeDesc;

    /** 真实名称 */
    @TableField("real_name")
    private  String realName;

    /** 手机号（登录账号，唯一） */
    @TableField("mobile")
    private String mobile;

    /** 密码（SHA-256 密文） */
    @TableField("password")
    private String password;

    /** 状态 */
    @TableField("status")
    private Integer status;

    /** 删除标记：0正常，1已删除 */
    @TableField("delete_flag")
    private Integer deleteFlag;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

    /** 创建人 */
    @TableField("create_name")
    private String createName;

    /** 更新时间 */
    @TableField("update_time")
    private Date updateTime;

    /** 更新人 */
    @TableField("update_name")
    private String updateName;
}
