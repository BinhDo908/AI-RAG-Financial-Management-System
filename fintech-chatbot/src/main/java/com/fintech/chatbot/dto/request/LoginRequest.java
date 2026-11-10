package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * UC-D02 — Đăng nhập.
 */
public record LoginRequest(
        @NotBlank(message = "Email không được để trống") String email,
        @NotBlank(message = "Mật khẩu không được để trống") String password
) {
}
