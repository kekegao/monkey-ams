package com.monkey.common.lock.autoconfigure;

import com.monkey.common.lock.aspect.DistributedLockAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class DistributedLockAutoConfiguration {

    @Bean
    public DistributedLockAspect distributedLockAspect() {
        return new DistributedLockAspect();
    }
}
