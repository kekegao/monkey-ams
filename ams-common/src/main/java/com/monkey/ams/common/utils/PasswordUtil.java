package com.monkey.ams.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码加密工具类（SHA-256 + 固定盐）
 *
 * @author gkk
 */
public class PasswordUtil {

    private static final String SALT = "monkey-ams@2026";

    /**
     * 密码加密：SHA-256(SALT + password)
     *
     * @param rawPassword 明文密码
     * @return 64位十六进制密文
     */
    public static String encrypt(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((SALT + rawPassword).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 校验明文密码与密文是否匹配
     *
     * @param rawPassword 明文密码
     * @param encoded     已加密的密文
     * @return true 匹配
     */
    public static boolean matches(String rawPassword, String encoded) {
        return encrypt(rawPassword).equals(encoded);
    }
}
