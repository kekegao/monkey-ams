package com.monkey.order.bsm.biz.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDto {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderId;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 货主id
     */
    private String shipperUserId;

    /**
     * 货主名称
     */
    private String shipperName;

    /**
     * 货主手机号
     */
    private String shipperMobile;

    /**
     * 承运方id
     */
    private String carrierUserId;

    /**
     * 承运方名称
     */
    private String carrierName;

    /**
     * 承运方手机号
     */
    private String carrierMobile;

    /**
     * 物品类型，例如建材，钢铁，煤炭
     */
    private String goodsType;

    /**
     * 物品描述
     */
    private String goodsDescription;

    /**
     * 物品重量
     */
    private BigDecimal goodsWeight;

    /**
     * 发货源省市区-省份
     */
    private String shipperProvince;

    /**
     * 发货源省市区-城市
     */
    private String shipperCity;

    /**
     * 发货源省市区-地区
     */
    private String shipperArea;

    /**
     * 发货源省市区-详细地址
     */
    private String shipperAddress;

    /**
     * 收货地省市区-省份
     */
    private String carrierProvince;

    /**
     * 收货地省市区-城市
     */
    private String carrierCity;

    /**
     * 收货地省市区-地区
     */
    private String carrierArea;

    /**
     * 收货地省市区-详细地址
     */
    private String carrierAddress;

    /**
     * 删除标记：0正常，1已删除
     */
    private Byte deleteFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
