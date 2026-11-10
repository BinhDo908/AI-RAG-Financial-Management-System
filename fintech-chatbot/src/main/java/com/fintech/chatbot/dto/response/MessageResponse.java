package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.InputMode;
import com.fintech.chatbot.entity.enums.MessageRole;

import java.time.LocalDateTime;

/**
 * Tin nhắn; {@code citations} là chuỗi JSON danh sách nguồn trích dẫn.
 */
public record MessageResponse(
        Long id,
        MessageRole role,
        String content,
        InputMode inputMode,
        String intent,
        String citations,
        LocalDateTime createdAt
) {
}
