package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.ai.AiTask;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * UC-C04 — Cấu hình AI. {@code effectiveModel}: model chat mặc định đang dùng;
 * {@code taskModels}: model thực tế của từng tác vụ sau khi áp thứ tự ưu tiên (Admin → biến môi trường → mặc định).
 */
public record AiConfigResponse(
        Map<String, String> values,
        Map<String, String> defaults,
        String effectiveModel,
        Map<AiTask, String> taskModels,
        ProviderStatus provider,
        List<HistoryItem> history
) {

    /** Trạng thái kết nối: không bao giờ trả API key, chỉ cho biết đã cấu hình hay chưa. */
    public record ProviderStatus(
            String chatBaseUrl,
            boolean chatReady,
            String embeddingModel,
            boolean embeddingReady,
            boolean serverSttReady
    ) {
    }

    public record HistoryItem(String key, String oldValue, String newValue, Long changedBy, LocalDateTime changedAt) {
    }
}
