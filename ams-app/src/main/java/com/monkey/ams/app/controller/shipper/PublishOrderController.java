package com.monkey.ams.app.controller.shipper;

import com.monkey.ams.app.controller.BaseController;
import com.monkey.ams.common.response.Result;
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
    public Result<List<OrderDto>> queryPublishOrderList(@RequestBody(required = false) OrderQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new OrderQueryDTO();
        }
        queryDTO.setShipperUserId(getUserId());
        return orderProtocol.queryPublishOrderList(queryDTO);
    }

    /**
     * 货主确认成交：摘单(2) -> 成交(3)
     *
     * POST /publishOrder/dealOrder
     */
    @PostMapping("/dealOrder")
    public Result dealOrder(@RequestBody OrderOperateDTO orderOperateDTO) {
        return orderProtocol.dealOrder(orderOperateDTO);
    }

    /**
     * 货主取消承运方摘单：摘单(2) -> 发布(1)，恢复等待摘单
     *
     * POST /publishOrder/cancelAccept
     */
    @PostMapping("/cancelAccept")
    public Result cancelAccept(@RequestBody OrderOperateDTO orderOperateDTO) {
        return orderProtocol.cancelAccept(orderOperateDTO);
    }

    /**
     * 货主回单确认：确认收货(5) -> 回单确认(6)，成功后释放承运方发货保证金
     *
     * POST /publishOrder/receiptConfirm
     */
    @PostMapping("/receiptConfirm")
    public Result receiptConfirm(@RequestBody OrderOperateDTO orderOperateDTO) {
        return orderProtocol.receiptConfirm(orderOperateDTO);
    }
}
