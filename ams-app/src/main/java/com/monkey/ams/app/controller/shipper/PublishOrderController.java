package com.monkey.ams.app.controller.shipper;

import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.protocol.OrderProtocol;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/publish")
public class PublishOrderController {


    @DubboReference
    private OrderProtocol orderProtocol;


    /**
     * 发布
     *
     * POST /publish/publishOrder
     */
    @PostMapping("/publishOrder")
    public Result publishOrder(@RequestBody OrderPublishDTO orderPublishDTO) {
        return orderProtocol.publishOrder(orderPublishDTO);
    }
}
