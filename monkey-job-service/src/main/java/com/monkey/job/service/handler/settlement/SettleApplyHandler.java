package com.monkey.job.service.handler.settlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SettleApplyHandler {


    /**
     * 每天凌晨 1 点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void handleReceiptConfirmOrders() {

        log.info("开始处理回单确认订单");

        // 查询 status = 6 的订单
        // 处理业务
    }
}
