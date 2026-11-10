package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * UC-D03 — Đổi mật khẩu.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
        String currentPassword,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$", message = "Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số")
        String newPassword,

        @NotBlank(message = "Vui lòng xác nhận mật khẩu mới")
        String confirmPassword
) {
}
