package com.monkey.user.bsm.biz;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;


@ImportResource("classpath:dubbo/dubbo-provider.xml")
@SpringBootApplication
@ComponentScan(basePackages = {"com.monkey.user.bsm.biz", "com.monkey.ams.common"})
@MapperScan("com.monkey.user.bsm.biz")
@EnableDubbo
public class UserBsmBizServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserBsmBizServiceApplication.class, args);
    }

}
