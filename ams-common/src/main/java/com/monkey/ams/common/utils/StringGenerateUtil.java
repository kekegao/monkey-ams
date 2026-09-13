package com.monkey.ams.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class StringGenerateUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 生成流水号：前缀 + 17位毫秒级日期时间戳（yyyyMMddHHmmssSSS）+ 6位随机数
     *
     * @param prefix 流水号前缀（如业务类型码 TX/RZ/YD 等），可为 null / 空串，不带额外分隔符
     * @return 前缀 + 23位流水号
     */
    public static String generateOrderNo(String prefix) {
        String dateTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1_000_000);
        String body = dateTime + String.format("%06d", random);
        return (prefix == null || prefix.isEmpty()) ? body : prefix + body;
    }

    /**
     * 生成订单号：17位毫秒级日期时间戳（yyyyMMddHHmmssSSS）+ 6位随机数
     *
     * @return 23位订单号
     */
    public static String generateOrderNo() {
        return generateOrderNo("");
    }

    

}
