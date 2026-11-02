package com.gdou.marinebio.common;

/**
 * 业务异常，由 GlobalExceptionHandler 统一转换为 Result.fail
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BizException(String message) {
        super(message);
    }
}
