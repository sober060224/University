package com.gdou.marinebio.common;

/**
 * 业务异常，由 GlobalExceptionHandler 统一转换为 Result.fail
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
