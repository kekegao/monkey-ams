package com.monkey.order.bsm.biz.service;

import com.monkey.ams.common.utils.SnowflakeIdWorker;
import com.monkey.order.bsm.biz.protocol.OrderProtocol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SpringBootTest
class OrderBsmBizServiceApplicationTests {

    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    @Autowired
    private OrderProtocol orderProtocol;

    @Test
    void contextLoads() {
    }

    @Test
    void testNextId() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            String id = snowflakeIdWorker.nextId();
            assert !id.isEmpty();
            assert ids.add(id);
        }
    }

    @Test
    void testInsertOrder() {
        Map<String, Object> param = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis());
        param.put("orderId",timestamp);
        orderProtocol.insertOrder(param);
    }

}
