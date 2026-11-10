package com.fintech.chatbot.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.chatbot.entity.enums.FeedbackRating;
import com.fintech.chatbot.entity.enums.FeedbackReason;
import com.fintech.chatbot.entity.enums.FeedbackRootCause;
import com.fintech.chatbot.entity.enums.ReviewStatus;

import java.time.LocalDateTime;

/**
 * UC-B09 / UC-C02.
 */
public final class FeedbackResponses {

    private FeedbackResponses() {
    }

    public record FeedbackView(Long id, Long messageId, FeedbackRating rating, FeedbackReason reason, String comment,
                               ReviewStatus reviewStatus, LocalDateTime createdAt) {
    }

    /** Hàng chờ kiểm duyệt (bước 1). */
    public record ReportSummary(Long id, Long messageId, String intent, FeedbackReason reason, String comment,
                                ReviewStatus reviewStatus, FeedbackRootCause rootCause, LocalDateTime createdAt) {
    }

    /**
     * Bước 2: chỉ gồm câu hỏi ngay trước và câu trả lời bị báo cáo, không có dữ liệu tài chính khác của user (UC-B09 BR).
     */
    public record ReportDetail(ReportSummary report, String question, String answer, JsonNode citations,
                               JsonNode retrieval, String adminNote) {
    }
}
