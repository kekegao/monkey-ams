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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 订单表
 * </p>
 *
 * @author gkk
 * @since 2026-08-25
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
     * 订单状态 1发布-2摘单-3成交-4发货-5确认收货-6回单确认-7结算申请-8结算-9对账-10发票
     */
    @TableField("status")
    private Integer status;

    /**
     * 货主id
     */
    @TableField("shipper_user_id")
    private String shipperUserId;

    /**
     * 货主用户名称
     */
    @TableField("shipper_user_name")
    private String shipperUserName;

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
     * 承运方用户名称
     */
    @TableField("carrier_user_name")
    private String carrierUserName;

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
     * 物品类型，例如建材，钢铁，煤炭
     */
    @TableField("goods_type")
    private String goodsType;

    /**
     * 物品描述
     */
    @TableField("goods_description")
    private String goodsDescription;

    /**
     * 物品重量
     */
    @TableField("goods_weight")
    private BigDecimal goodsWeight;

    /**
     * 运费金额
     */
    @TableField("transport_money")
    private BigDecimal transportMoney;

    /**
     * 发货源省市区-省份
     */
    @TableField("shipper_province")
    private String shipperProvince;

    /**
     * 发货源省市区-城市
     */
    @TableField("shipper_city")
    private String shipperCity;

    /**
     * 发货源省市区-地区
     */
    @TableField("shipper_area")
    private String shipperArea;

    /**
     * 发货源省市区-详细地址
     */
    @TableField("shipper_address")
    private String shipperAddress;

    /**
     * 收货地省市区-省份
     */
    @TableField("carrier_province")
    private String carrierProvince;

    /**
     * 收货地省市区-城市
     */
    @TableField("carrier_city")
    private String carrierCity;

    /**
     * 收货地省市区-地区
     */
    @TableField("carrier_area")
    private String carrierArea;

    /**
     * 收货地省市区-详细地址
     */
    @TableField("carrier_address")
    private String carrierAddress;

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
