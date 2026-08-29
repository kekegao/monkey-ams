package com.monkey.acq.bsm.biz;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@ImportResource("classpath:dubbo/dubbo-provider.xml")
@SpringBootApplication
@EnableDubbo
public class AcqBsmBizServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcqBsmBizServiceApplication.class, args);
    }

}
