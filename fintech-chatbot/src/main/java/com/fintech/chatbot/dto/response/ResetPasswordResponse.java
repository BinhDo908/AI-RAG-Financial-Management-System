package com.fintech.chatbot.dto.response;

/**
 * UC-C05 — Mật khẩu tạm thời trả về một lần duy nhất cho Admin.
 */
public record ResetPasswordResponse(Long userId, String temporaryPassword) {
}
