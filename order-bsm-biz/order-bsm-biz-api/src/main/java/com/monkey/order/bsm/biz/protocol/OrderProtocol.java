package com.monkey.order.bsm.biz.protocol;


import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.AcceptOrderDTO;
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

    /**
     * 摘单
     *
     * @param acceptOrderDTO
     * @return
     */
    Result acceptOrder(AcceptOrderDTO acceptOrderDTO);

    void insertOrder(Map<String,Object> param);
}
