package com.fintech.chatbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kết nối tới nhà cung cấp AI tương thích OpenAI API (mặc định Vilao AI).
 * Mỗi năng lực (chat, embedding, giọng nói) cấu hình riêng (URL, key, model) để đổi độc lập từng phần.
 * {@code tasks}: model mặc định cho từng tác vụ LLM (để trống = dùng {@code chat.model}); Admin ghi đè ở UC-C04.
 * {@code hedgeAfterMs} + {@code fallbackModel}: model chính chưa trả lời sau {@code hedgeAfterMs} thì gửi thêm yêu cầu
 * tới model dự phòng (nhà cung cấp khác), lấy kết quả về trước; 0 = tắt. Model chính lỗi tạm thời thì chuyển ngay.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(int timeoutSeconds, int hedgeAfterMs, String fallbackModel,
                           Endpoint chat, Endpoint embedding, Endpoint stt, Tasks tasks) {

    public boolean hasFallbackModel() {
        return fallbackModel != null && !fallbackModel.isBlank();
    }

    public record Endpoint(String baseUrl, String apiKey, String model) {

        public boolean hasKey() {
            return apiKey != null && !apiKey.isBlank();
        }

        public boolean hasModel() {
            return model != null && !model.isBlank();
        }

        /** Server chạy trên máy (localhost) không cần API key. */
        public boolean isLocal() {
            return baseUrl != null && (baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1"));
        }

        public boolean usable() {
            return baseUrl != null && !baseUrl.isBlank() && (hasKey() || isLocal());
        }
    }

    public record Tasks(String router, String extraction, String answer, String advice, String judge) {
    }
}
