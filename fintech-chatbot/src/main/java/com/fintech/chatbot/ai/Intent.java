package com.fintech.chatbot.ai;

/**
 * Ý định của tin nhắn (USE_CASES.md §9.1).
 */
public enum Intent {
    /** UC-A11 / A12 — ghi giao dịch. */
    RECORD_TRANSACTION,
    /** UC-A13 — hỏi số liệu cá nhân. */
    QUERY_PERSONAL_DATA,
    /** UC-B01 — hỏi kiến thức tài chính (RAG). */
    KNOWLEDGE_QA,
    /** UC-B02 — tư vấn cá nhân hóa. */
    PERSONAL_ADVICE,
    /** UC-B07 — tỷ giá, giá vàng, lãi suất. */
    MARKET_DATA,
    SMALL_TALK,
    OUT_OF_SCOPE,
    /** Độ tin cậy thấp → hỏi lại user muốn ghi giao dịch hay hỏi thông tin. */
    CLARIFY
}
