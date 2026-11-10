package com.fintech.chatbot.dto.response;

import java.time.LocalDateTime;

/**
 * Hội thoại (không kèm tin nhắn).
 */
public record ConversationResponse(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
