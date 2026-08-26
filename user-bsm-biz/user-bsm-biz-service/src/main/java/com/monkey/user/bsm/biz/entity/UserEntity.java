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

    /** 用户ID（雪花算法生成） */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /** 用户名 */
    @TableField("user_name")
    private String userName;

    /** 手机号（登录账号，唯一） */
    @TableField("mobile")
    private String mobile;

    /** 密码（SHA-256 密文） */
    @TableField("password")
    private String password;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;
}
