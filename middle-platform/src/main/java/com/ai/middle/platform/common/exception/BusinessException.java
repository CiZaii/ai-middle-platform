package com.ai.middle.platform.common.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
