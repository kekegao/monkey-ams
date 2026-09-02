package com.monkey.ams.app.controller.carrier;

import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.AcceptOrderDTO;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.protocol.OrderProtocol;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accept")
public class AcceptOrderController {


    @DubboReference
    private OrderProtocol orderProtocol;


    /**
     * 摘单
     *
     * POST /accept/acceptOrder
     */
    @PostMapping("/acceptOrder")
    public Result acceptOrder(@RequestBody AcceptOrderDTO acceptOrderDTO) {
        return orderProtocol.acceptOrder(acceptOrderDTO);
    }
}
