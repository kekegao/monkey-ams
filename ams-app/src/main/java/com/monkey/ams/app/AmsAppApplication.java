package com.monkey.ams.app;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class AmsAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmsAppApplication.class, args);
    }

}
