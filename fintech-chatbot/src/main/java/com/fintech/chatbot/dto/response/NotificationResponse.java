package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * Thông báo trong ứng dụng.
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String content,
        Long refId,
        boolean read,
        LocalDateTime createdAt
) {
}
