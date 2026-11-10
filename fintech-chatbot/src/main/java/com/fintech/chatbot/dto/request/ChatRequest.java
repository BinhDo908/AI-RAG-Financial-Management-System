package com.fintech.chatbot.dto.request;

import com.fintech.chatbot.entity.enums.InputMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Tin nhắn gửi chatbot (§9.1). {@code conversationId} null = hội thoại mới;
 * {@code inputMode} VOICE khi nội dung là transcript giọng nói (UC-A12, B03).
 */
public record ChatRequest(
        Long conversationId,
        @NotBlank(message = "Tin nhắn không được để trống") @Size(max = 2000, message = "Tin nhắn tối đa 2000 ký tự") String message,
        InputMode inputMode
) {
}
