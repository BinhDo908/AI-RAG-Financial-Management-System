package com.fintech.chatbot.entity.enums;

/**
 * Nguyên nhân câu trả lời sai do Admin phân loại (UC-C02 bước 3).
 */
public enum FeedbackRootCause {
    /** Kho tri thức chưa có tài liệu → bổ sung ở UC-C01. */
    MISSING_DOCUMENT,
    /** Có tài liệu nhưng truy xuất sai đoạn → chỉnh top_k / ngưỡng ở UC-C04. */
    WRONG_RETRIEVAL,
    /** Truy xuất đúng nhưng LLM diễn đạt sai → chỉnh prompt / model. */
    LLM_WRONG,
    /** Số liệu cá nhân sai (BR-06). */
    WRONG_NUMBER,
    /** Báo cáo không đúng. */
    INVALID_REPORT
}
