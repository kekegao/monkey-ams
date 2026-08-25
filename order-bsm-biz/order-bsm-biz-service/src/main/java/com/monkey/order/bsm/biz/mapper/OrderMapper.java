package com.monkey.order.bsm.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.monkey.order.bsm.biz.dto.OrderDto;
import com.monkey.order.bsm.biz.entity.Order;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 Mapper 接口
 * </p>
 *
 * @author gkk
 * @since 2026-08-17
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {


    List<OrderDto> selectListByCondition(Map<String, Object> condition);

}
