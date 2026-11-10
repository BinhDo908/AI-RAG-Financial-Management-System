package com.fintech.chatbot.ai.client;

import java.util.List;

/**
 * Chuyển văn bản thành vector (dùng cho RAG, UC-B01 / UC-C01).
 */
public interface EmbeddingClient {

    /** Trả về một vector cho mỗi văn bản, đúng thứ tự đầu vào. */
    List<float[]> embed(List<String> texts);

    /** Tên model embedding đang dùng; lưu cùng vector để phát hiện cần index lại khi đổi model. */
    String modelName();

    boolean embeddingAvailable();
}
