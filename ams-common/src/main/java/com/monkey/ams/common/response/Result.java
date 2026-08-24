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

}
