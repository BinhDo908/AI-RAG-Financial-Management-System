package com.fintech.chatbot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.chatbot.entity.enums.InputMode;
import com.fintech.chatbot.entity.enums.MessageRole;

import java.time.LocalDateTime;

/**
 * Tin nhắn hiển thị trong khung chat. {@code citations}: danh sách nguồn; {@code payload}: dữ liệu có cấu trúc
 * (type = TRANSACTION_DRAFTS | PERSONAL_DATA | FINANCIAL_SUMMARY | FX_RATE | CLARIFY | TRANSACTIONS_SAVED).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageView(
        Long id,
        MessageRole role,
        String content,
        InputMode inputMode,
        String intent,
        JsonNode citations,
        JsonNode payload,
        Boolean noSource,
        Integer latencyMs,
        LocalDateTime createdAt
) {
}
