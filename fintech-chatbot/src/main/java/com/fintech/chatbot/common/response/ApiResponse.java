package com.fintech.chatbot.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Định dạng phản hồi thống nhất cho mọi API.
 * Thành công: {success: true, data: ...}; lỗi: {success: false, errorCode, message}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String errorCode,
        Object details,
        LocalDateTime timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, null, message, null, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> error(String errorCode, String message, Object details) {
        return new ApiResponse<>(false, null, message, errorCode, details, LocalDateTime.now());
    }
}
