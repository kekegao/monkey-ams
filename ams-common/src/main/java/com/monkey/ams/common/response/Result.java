package com.monkey.ams.common.response;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;


@Getter
@Setter
@Component
public class Result<T> extends HashMap<String, Object> {

	private static final long serialVersionUID = 1L;

    private String code;
    private String message;
    private T data;

    /**
     * 生成成功的 Result 对象
     */
    public static <T> Result<T> success() {
        return build("200", "操作成功", null);
    }

    /**
     * 生成成功的 Result 对象（携带数据）
     */
    public static <T> Result<T> success(T data) {
        return build("200", "操作成功", data);
    }

    /**
     * 生成成功的 Result 对象（携带数据与自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return build("200", message, data);
    }

    /**
     * 生成失败的 Result 对象
     */
    public static <T> Result<T> fail() {
        return build("500", "操作失败", null);
    }

    /**
     * 生成失败的 Result 对象（自定义消息）
     */
    public static <T> Result<T> fail(String message) {
        return build("500", message, null);
    }

    /**
     * 生成失败的 Result 对象（自定义状态码与消息）
     */
    public static <T> Result<T> fail(String code, String message) {
        return build(code, message, null);
    }

    private static <T> Result<T> build(String code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);
        return result;
    }

}
