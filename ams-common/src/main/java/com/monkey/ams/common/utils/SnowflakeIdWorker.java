package com.monkey.ams.common.utils;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdWorker {

    // 起始时间戳（2020-01-01 00:00:00），可根据实际情况调整
    private final long twepoch = 1577808000000L;

    // 机器ID占用位数
    private final long workerIdBits = 5L;
    // 数据中心ID占用位数
    private final long datacenterIdBits = 5L;
    // 序列号占用位数
    private final long sequenceBits = 12L;

    // 最大支持机器数 31
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    // 最大支持数据中心数 31
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    // 各部分的偏移量
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    // 序列号掩码 4095
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdWorker() {
        // 默认使用 0 和 0，生产环境应从配置文件或ZooKeeper中获取
        this(0, 0);
    }

    public SnowflakeIdWorker(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("workerId不能大于 %d 或小于 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenterId不能大于 %d 或小于 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    // 核心方法：生成ID
    public synchronized String nextId() {
        long timestamp = timeGen();

        // 时钟回拨处理
        if (timestamp < lastTimestamp) {
            // 如果时钟回拨时间小于5ms，可以等待；否则抛出异常
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException("时钟回拨异常，无法生成ID");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("时钟回拨异常，无法生成ID");
            }
        }

        if (timestamp == lastTimestamp) {
            // 同一毫秒内，序列号递增
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 序列号用尽，等待下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组合生成最终ID
        Long uuid = ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
        return uuid.toString();
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    /** 62进制字符集（数字 + 大小写字母），用于生成用户名 */
    private static final char[] BASE62_CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /** 用户名固定长度 */
    private static final int USERNAME_LENGTH = 12;

    /**
     * 生成用户名：字母 + 数字组合，固定12位，基于雪花ID转62进制保证唯一
     *
     * @return 12位用户名
     */
    public String generateUserName() {
        long id = Long.parseLong(nextId());
        StringBuilder sb = new StringBuilder();
        // 雪花ID为64位long，转62进制最多11位，左侧补'0'至12位，完整保留唯一性
        while (id > 0 && sb.length() < USERNAME_LENGTH) {
            sb.append(BASE62_CHARS[(int) (id % 62)]);
            id /= 62;
        }
        // 不足12位时左侧补 '0'
        while (sb.length() < USERNAME_LENGTH) {
            sb.append('0');
        }
        return sb.reverse().toString();
    }
}
