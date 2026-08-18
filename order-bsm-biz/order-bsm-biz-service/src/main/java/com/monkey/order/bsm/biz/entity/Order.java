package com.monkey.order.bsm.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 订单表
 * </p>
 *
 * @author gkk
 * @since 2026-08-17
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("tf_b_order")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    @TableField("order_id")
    private String orderId;

    /**
     * 订单状态
     */
    @TableField("status")
    private Integer status;

    /**
     * 货主id
     */
    @TableField("shipper_user_id")
    private String shipperUserId;

    /**
     * 货主名称
     */
    @TableField("shipper_name")
    private String shipperName;

    /**
     * 货主手机号
     */
    @TableField("shipper_mobile")
    private String shipperMobile;

    /**
     * 承运方id
     */
    @TableField("carrier_user_id")
    private String carrierUserId;

    /**
     * 承运方名称
     */
    @TableField("carrier_name")
    private String carrierName;

    /**
     * 承运方手机号
     */
    @TableField("carrier_mobile")
    private String carrierMobile;

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
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;
}
