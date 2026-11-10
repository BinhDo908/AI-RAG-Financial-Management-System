package com.fintech.chatbot.ai;

/**
 * Các tác vụ gọi LLM; mỗi tác vụ cấu hình được một model riêng (UC-C04, khóa {@code llm.model.<task>}).
 * Để trống thì dùng model chat mặc định.
 */
public enum AiTask {
    /** Phân loại ý định (§9.1) — cần chính xác, câu trả lời rất ngắn. */
    ROUTER("llm.model.router"),
    /** Trích xuất JSON: giao dịch (A11), truy vấn dữ liệu (A13), thông tin tỷ giá (B07). */
    EXTRACTION("llm.model.extraction"),
    /** Sinh câu trả lời: kiến thức RAG (B01) và diễn đạt số liệu (A13). */
    ANSWER("llm.model.answer"),
    /** Tư vấn cá nhân hóa (B02). */
    ADVICE("llm.model.advice"),
    /** Chấm điểm câu trả lời trong bộ đánh giá (C03) — nên khác model trả lời để tránh tự chấm cho mình. */
    JUDGE("llm.model.judge");

    private final String configKey;

    AiTask(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
