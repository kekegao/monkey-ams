package com.monkey.job.service.handler.settlement;

import com.monkey.ams.common.response.Result;
import com.monkey.order.bsm.biz.protocol.OrderJobProtocol;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SettleApplyHandler {


    @DubboReference
    private OrderJobProtocol orderJobProtocol;

    /**
     * 每天凌晨 1 点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void handleReceiptConfirmOrders() {

        log.info("结算申请定时任务开始");
        // 查询 status = 6 的订单
        // 处理业务
        Result result = orderJobProtocol.settleApplyTask();
        if (!result.isSuccess()) log.error("结算申请定时任结果:{}", result.getMessage());
        log.info("结算申请定时任务结束");
    }
}
