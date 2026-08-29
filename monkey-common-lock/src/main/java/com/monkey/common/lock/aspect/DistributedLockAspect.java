package com.monkey.common.lock.aspect;


import com.monkey.common.lock.annotation.DistributedLock;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
public class DistributedLockAspect {

    @Resource
    private RedissonClient redissonClient;

    private final ExpressionParser parser =
            new SpelExpressionParser();

    private final DefaultParameterNameDiscoverer discoverer =
            new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = parseKey(
                distributedLock.key(),
                joinPoint
        );

        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;

        try {

            if (distributedLock.leaseTime() == -1) {

                locked = lock.tryLock(
                        distributedLock.waitTime(),
                        distributedLock.timeUnit()
                );

            } else {

                locked = lock.tryLock(
                        distributedLock.waitTime(),
                        distributedLock.leaseTime(),
                        distributedLock.timeUnit()
                );
            }

            if (!locked) {
                throw new RuntimeException(
                        "获取分布式锁失败: " + lockKey
                );
            }

            return joinPoint.proceed();

        } finally {

            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String parseKey(
            String key,
            ProceedingJoinPoint joinPoint) {

        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();

        Method method =
                signature.getMethod();

        String[] parameterNames =
                discoverer.getParameterNames(method);

        Object[] args =
                joinPoint.getArgs();

        EvaluationContext context =
                new StandardEvaluationContext();

        if (parameterNames != null) {

            for (int i = 0;
                 i < parameterNames.length;
                 i++) {

                context.setVariable(
                        parameterNames[i],
                        args[i]
                );
            }
        }

        return parser
                .parseExpression(key)
                .getValue(
                        context,
                        String.class
                );
    }
}
