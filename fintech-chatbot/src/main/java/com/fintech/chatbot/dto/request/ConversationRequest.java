package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.Size;

/**
 * UC-B08 — Tạo hội thoại (tiêu đề tùy chọn) hoặc đổi tên (bắt buộc tiêu đề).
 */
public record ConversationRequest(
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String title
) {
}
