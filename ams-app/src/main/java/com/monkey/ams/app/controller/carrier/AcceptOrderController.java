package com.monkey.ams.app.controller.carrier;

import com.monkey.ams.app.controller.BaseController;
import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.dto.AcceptOrderDTO;
import com.monkey.order.bsm.biz.dto.OrderDto;
import com.monkey.order.bsm.biz.dto.OrderOperateDTO;
import com.monkey.order.bsm.biz.dto.OrderPublishDTO;
import com.monkey.order.bsm.biz.dto.OrderQueryDTO;
import com.monkey.order.bsm.biz.protocol.OrderProtocol;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accept")
public class AcceptOrderController extends BaseController {


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

    /**
     * 承运方确认发货：成交(3) -> 发货(4)，启动实际运输
     *
     * POST /accept/shipOrder
     */
    @PostMapping("/shipOrder")
    public Result shipOrder(@RequestBody OrderOperateDTO orderOperateDTO) {
        return orderProtocol.shipOrder(orderOperateDTO);
    }

    /**
     * 确认收货：发货(4) -> 确认收货(5)，确认货物已送达
     *
     * POST /accept/confirmReceipt
     */
    @PostMapping("/confirmReceipt")
    public Result confirmReceipt(@RequestBody OrderOperateDTO orderOperateDTO) {
        return orderProtocol.confirmReceipt(orderOperateDTO);
    }

    /**
     * 承运端货源大厅列表（可摘货源 / 线路搜索）
     *
     * POST /accept/list
     */
    @PostMapping("/list")
    public Result<List<OrderDto>> querySourceOrderList(@RequestBody(required = false) OrderQueryDTO queryDTO) {
        return orderProtocol.querySourceOrderList(queryDTO);
    }

    /**
     * 承运端「我的运单」列表：查询当前承运方所有已摘的运单（status >= 2）
     *
     * POST /accept/myOrders
     */
    @PostMapping("/myOrders")
    public Result<List<OrderDto>> queryCarrierOrderList(@RequestBody(required = false) OrderQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new OrderQueryDTO();
        }
        queryDTO.setCarrierUserId(getUserId());
        return orderProtocol.queryCarrierOrderList(queryDTO);
    }
}
