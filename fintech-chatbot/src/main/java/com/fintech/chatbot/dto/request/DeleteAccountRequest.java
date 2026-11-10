package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * UC-D03 AF-2 — Xóa tài khoản: nhập lại mật khẩu và gõ đúng câu xác nhận.
 */
public record DeleteAccountRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu") String password,
        @NotBlank(message = "Vui lòng nhập câu xác nhận") String confirmation
) {
    public static final String CONFIRMATION_TEXT = "XOA TAI KHOAN";
}
