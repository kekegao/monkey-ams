package com.monkey.order.bsm.biz;


import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@ImportResource("classpath:dubbo/dubbo-provider.xml")
@SpringBootApplication
@ComponentScan(basePackages = {"com.monkey.order.bsm.biz","com.monkey.ams.common","com.monkey.common.lock"})
@EnableDubbo
public class OrderBsmBizServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderBsmBizServiceApplication.class, args);
    }

}
