package com.fintech.chatbot.common.exception;

import lombok.Getter;

/**
 * Lỗi nghiệp vụ có mã lỗi; được GlobalExceptionHandler chuyển thành ApiResponse.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static BusinessException notFound(String what) {
        return new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy " + what);
    }
}
