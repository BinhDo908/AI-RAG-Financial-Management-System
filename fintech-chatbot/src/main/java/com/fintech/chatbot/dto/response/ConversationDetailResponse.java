package com.fintech.chatbot.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hội thoại kèm toàn bộ tin nhắn (UC-B08 bước 2): trích dẫn, thẻ giao dịch đã lưu… nằm trong từng tin nhắn.
 */
public record ConversationDetailResponse(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ChatMessageView> messages
) {
}
