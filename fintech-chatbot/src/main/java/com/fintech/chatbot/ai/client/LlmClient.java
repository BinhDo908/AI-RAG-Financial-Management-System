package com.fintech.chatbot.ai.client;

import java.util.List;
import java.util.function.Consumer;

/**
 * Gọi mô hình ngôn ngữ. Cài đặt mặc định: {@link OpenAiCompatibleClient}; test dùng bản giả lập.
 */
public interface LlmClient {

    /**
     * @param model       id model của nhà cung cấp, ví dụ "gpt-4o-mini"
     * @param temperature 0 = ổn định nhất
     * @return nội dung trả lời của assistant
     */
    String chat(String model, List<ChatMessage> messages, double temperature);

    /**
     * Như {@link #chat} nhưng đẩy từng đoạn chữ cho {@code onDelta} ngay khi nhà cung cấp gửi về (SSE).
     * Mặc định: chờ đủ câu rồi đẩy một lần (dùng cho bản giả lập trong test).
     *
     * @return toàn bộ nội dung trả lời
     */
    default String stream(String model, List<ChatMessage> messages, double temperature, Consumer<String> onDelta) {
        String text = chat(model, messages, temperature);
        onDelta.accept(text);
        return text;
    }

    /**
     * Gọi model đọc được ảnh: một lời nhắc hệ thống + một tin nhắn gồm chữ và ảnh (UC-A14 quét hóa đơn).
     *
     * @param mimeType ví dụ "image/jpeg"
     */
    default String vision(String model, String system, String text, byte[] image, String mimeType, double temperature) {
        throw new UnsupportedOperationException("Model không hỗ trợ đọc ảnh");
    }

    /** Có đủ cấu hình (URL + key) để gọi hay không. */
    boolean available();
}
