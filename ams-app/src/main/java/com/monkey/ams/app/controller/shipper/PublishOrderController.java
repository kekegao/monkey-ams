package com.monkey.ams.app.controller.shipper;

import com.monkey.ams.app.controller.BaseController;
import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.OrderDto;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.dto.OrderQueryDTO;
import com.monkey.order.bsm.biz.protocol.OrderProtocol;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 货主端 - 货源发布 / 我的发布订单
 */
@RestController
@RequestMapping("/publishOrder")
public class PublishOrderController extends BaseController {

    @DubboReference
    private OrderProtocol orderProtocol;

    /**
     * 发布运单
     *
     * POST /publishOrder/publish
     */
    @PostMapping("/publish")
    public Result publishOrder(@RequestBody OrderPublishDTO orderPublishDTO) {
        return orderProtocol.publishOrder(orderPublishDTO);
    }

    /**
     * 货主「我的订单」列表：查询当前登录货主发布的订单
     *
     * POST /publishOrder/list
     */
    @PostMapping("/list")
    public Result<List<OrderDto>> queryPublishOrderList() {
        OrderQueryDTO queryDTO = new OrderQueryDTO();
        queryDTO.setShipperUserId(getUserId());
        return orderProtocol.queryPublishOrderList(queryDTO);
    }
}
