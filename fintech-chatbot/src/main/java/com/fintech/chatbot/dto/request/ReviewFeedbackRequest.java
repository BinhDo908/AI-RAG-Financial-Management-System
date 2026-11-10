package com.fintech.chatbot.dto.request;

import com.fintech.chatbot.entity.enums.FeedbackRootCause;
import com.fintech.chatbot.entity.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-C02 bước 3–5 — Phân loại nguyên nhân và đóng báo cáo (RESOLVED hoặc REJECTED).
 */
public record ReviewFeedbackRequest(
        @NotNull(message = "Vui lòng chọn trạng thái") ReviewStatus status,
        FeedbackRootCause rootCause,
        @Size(max = 1000) String adminNote
) {
}
