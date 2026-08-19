package com.monkey.order.bsm.biz;


import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.monkey.order.bsm.biz","com.monkey.ams.common"})
@EnableDubbo
public class OrderBsmBizServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderBsmBizServiceApplication.class, args);
    }

}
