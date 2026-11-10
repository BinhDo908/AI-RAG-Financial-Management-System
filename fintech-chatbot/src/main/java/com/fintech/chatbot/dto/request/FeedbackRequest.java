package com.fintech.chatbot.dto.request;

import com.fintech.chatbot.entity.enums.FeedbackRating;
import com.fintech.chatbot.entity.enums.FeedbackReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-B09 — 👍 / 👎 cho câu trả lời; 👎 kèm lý do sẽ vào hàng chờ kiểm duyệt của Admin.
 */
public record FeedbackRequest(
        @NotNull(message = "Vui lòng chọn đánh giá") FeedbackRating rating,
        FeedbackReason reason,
        @Size(max = 1000) String comment
) {
}
