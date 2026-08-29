package com.monkey.order.bsm.biz.protocol;


import com.monkey.ams.common.response.Result;

import java.util.Map;

public interface OrderProtocol {

    /**
     * 发布运单
     *
     * @param param
     * @return
     */
    Result publishOrder(Map<String,Object> param);

    void insertOrder(Map<String,Object> param);
}
