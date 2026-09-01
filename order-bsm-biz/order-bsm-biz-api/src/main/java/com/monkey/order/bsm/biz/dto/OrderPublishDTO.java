package com.monkey.order.bsm.biz.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 发布运单请求参数
 *
 * @author gkk
 */
@Data
public class OrderPublishDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发货人用户ID */
    private String shipperUserId;

    /** 发货人姓名 */
    private String shipperName;

    /** 发货人手机号 */
    private String shipperMobile;

    /** 发货人详细地址 */
    private String shipperAddress;

    /** 发货省 */
    private String shipperProvince;

    /** 发货市 */
    private String shipperCity;

    /** 发货区/县 */
    private String shipperArea;

    /** 收货省 */
    private String carrierProvince;

    /** 收货市 */
    private String carrierCity;

    /** 收货区/县 */
    private String carrierArea;

    /** 收货人详细地址 */
    private String carrierAddress;

    /** 货物类型 */
    private String goodsType;

    /** 货物描述 */
    private String goodsDescription;

    /** 货物重量 */
    private BigDecimal goodsWeight;

    /** 运费 */
    private BigDecimal transportMoney;
}
