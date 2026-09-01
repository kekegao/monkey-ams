package com.monkey.ams.app.controller;

import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.protocol.OrderProtocol;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/publishOrder")
public class PublishOrderController {


    @DubboReference
    private OrderProtocol orderProtocol;


    /**
     * 发布
     *
     * POST /publishOrder/publish
     */
    @PostMapping("/publish")
    public Result publishOrder(@RequestBody OrderPublishDTO orderPublishDTO) {
        return orderProtocol.publishOrder(orderPublishDTO);
    }
}
