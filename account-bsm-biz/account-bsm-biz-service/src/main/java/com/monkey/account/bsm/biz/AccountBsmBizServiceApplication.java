package com.monkey.account.bsm.biz;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;


@SpringBootApplication
@ComponentScan(basePackages = {"com.monkey.account.bsm.biz","com.monkey.ams.common","com.monkey.common.lock"})
@EnableDubbo
public class AccountBsmBizServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountBsmBizServiceApplication.class, args);
    }

}
