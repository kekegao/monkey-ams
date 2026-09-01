package com.monkey.order.bsm.biz.protocol;


import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;

import java.util.Map;

public interface OrderProtocol {

    /**
     * 发布运单
     *
     * @param orderPublishDTO 发布运单请求参数
     * @return
     */
    Result publishOrder(OrderPublishDTO orderPublishDTO);

    void insertOrder(Map<String,Object> param);
}
