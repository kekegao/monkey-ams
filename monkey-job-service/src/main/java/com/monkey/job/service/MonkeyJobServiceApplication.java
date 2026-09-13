package com.monkey.job.service;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableDubbo
public class MonkeyJobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonkeyJobServiceApplication.class, args);
    }

}
