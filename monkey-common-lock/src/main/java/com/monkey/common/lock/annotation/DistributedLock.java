package com.monkey.common.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 锁的 Key
     * 支持 SpEL
     *
     * 例如：
     * 'order:create:' + #orderId
     */
    String key();

    /**
     * 获取锁最大等待时间
     */
    long waitTime() default 3;

    /**
     * 锁租期
     *
     * -1：使用 Redisson WatchDog 自动续期
     */
    long leaseTime() default -1;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
