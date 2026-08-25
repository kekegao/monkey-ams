package com.monkey.ams.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class StringGenerateUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 生成订单号：前17位为毫秒级日期时间戳（yyyyMMddHHmmssSSS），后6位为随机数
     *
     * @return 23位订单号
     */
    public static String generateOrderNo() {
        String dateTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1_000_000);
        return dateTime + String.format("%06d", random);
    }

}
