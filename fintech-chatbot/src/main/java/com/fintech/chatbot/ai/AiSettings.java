package com.fintech.chatbot.ai;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.config.properties.AiProperties;
import com.fintech.chatbot.dao.AiConfigDao;
import com.fintech.chatbot.entity.AiConfigEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc tham số AI do Admin cấu hình (UC-C04, bảng ai_config) kèm giá trị mặc định và phạm vi hợp lệ (NFR-08).
 */
@Component
@RequiredArgsConstructor
public class AiSettings {

    public static final String LLM_MODEL = "llm.model";
    public static final String LLM_TEMPERATURE = "llm.temperature";
    public static final String RAG_TOP_K = "rag.top_k";
    public static final String RAG_TOP_N = "rag.top_n";
    public static final String RAG_SCORE_THRESHOLD = "rag.score_threshold";
    public static final String STT_LANGUAGE = "stt.language";
    public static final String DISCLAIMER = "chat.disclaimer";
    public static final String BLOCKED_TOPICS = "chat.blocked_topics";

    /** Giá trị mặc định (dùng cho "Khôi phục mặc định", UC-C04 AF-1). */
    public static final Map<String, String> DEFAULTS;

    static {
        Map<String, String> d = new LinkedHashMap<>();
        d.put(LLM_MODEL, "");
        for (AiTask task : AiTask.values()) {
            d.put(task.configKey(), "");
        }
        d.put(LLM_TEMPERATURE, "0.2");
        d.put(RAG_TOP_K, "8");
        d.put(RAG_TOP_N, "4");
        // text-embedding-3-large: đoạn liên quan ≈ 0,57–0,72, không liên quan ≈ 0,23–0,31 (đo 23/10/2026)
        d.put(RAG_SCORE_THRESHOLD, "0.45");
        d.put(STT_LANGUAGE, "vi-VN");
        d.put(DISCLAIMER, "Thông tin chỉ mang tính tham khảo và giáo dục, không phải lời khuyên đầu tư. "
                + "Hãy cân nhắc tình hình cá nhân trước khi quyết định.");
        d.put(BLOCKED_TOPICS, "khuyến nghị mua bán mã cổ phiếu cụ thể,khuyến nghị mua bán tiền mã hóa cụ thể,"
                + "vay nặng lãi,cờ bạc");
        DEFAULTS = java.util.Collections.unmodifiableMap(d);
    }

    private final AiConfigDao aiConfigDao;
    private final AiProperties aiProperties;

    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        Map<String, String> values = new LinkedHashMap<>(DEFAULTS);
        for (AiConfigEntry e : aiConfigDao.findAllEntries()) {
            values.put(e.getConfigKey(), e.getConfigValue());
        }
        String model = firstNonBlank(values.get(LLM_MODEL), aiProperties.chat().model());
        Map<AiTask, String> taskModels = new java.util.EnumMap<>(AiTask.class);
        for (AiTask task : AiTask.values()) {
            // Thứ tự ưu tiên: cấu hình Admin cho tác vụ → biến môi trường của tác vụ → model chat mặc định
            taskModels.put(task, firstNonBlank(values.get(task.configKey()), propertyModel(task), model));
        }
        return new Snapshot(
                model,
                taskModels,
                parseDouble(values.get(LLM_TEMPERATURE), 0.2),
                parseInt(values.get(RAG_TOP_K), 8),
                parseInt(values.get(RAG_TOP_N), 4),
                parseDouble(values.get(RAG_SCORE_THRESHOLD), 0.45),
                values.get(STT_LANGUAGE),
                values.get(DISCLAIMER),
                Arrays.stream(values.getOrDefault(BLOCKED_TOPICS, "").split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList(),
                values);
    }

    private String propertyModel(AiTask task) {
        AiProperties.Tasks t = aiProperties.tasks();
        if (t == null) {
            return null;
        }
        return switch (task) {
            case ROUTER -> t.router();
            case EXTRACTION -> t.extraction();
            case ANSWER -> t.answer();
            case ADVICE -> t.advice();
            case JUDGE -> t.judge();
        };
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /** Kiểm tra một giá trị trước khi lưu (UC-C04 EX-1). */
    public static String validate(String key, String value) {
        if (!DEFAULTS.containsKey(key)) {
            throw new BusinessException(ErrorCode.AI_CONFIG_INVALID, "Không có tham số " + key);
        }
        String v = value == null ? "" : value.trim();
        switch (key) {
            case LLM_TEMPERATURE -> requireRange(key, v, 0, 1);
            case RAG_SCORE_THRESHOLD -> requireRange(key, v, 0, 1);
            case RAG_TOP_K -> requireIntRange(key, v, 1, 20);
            case RAG_TOP_N -> requireIntRange(key, v, 1, 10);
            case DISCLAIMER -> {
                if (v.isEmpty() || v.length() > 1000) {
                    throw new BusinessException(ErrorCode.AI_CONFIG_INVALID, "Disclaimer không được trống, tối đa 1000 ký tự");
                }
            }
            default -> {
                // llm.model, llm.model.<tác vụ>, stt.language, chat.blocked_topics
                if (v.length() > 2000) {
                    throw new BusinessException(ErrorCode.AI_CONFIG_INVALID, key + " quá dài");
                }
            }
        }
        return v;
    }

    private static void requireRange(String key, String v, double min, double max) {
        try {
            double d = Double.parseDouble(v);
            if (d < min || d > max) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.AI_CONFIG_INVALID, "%s phải trong [%s, %s]".formatted(key, min, max));
        }
    }

    private static void requireIntRange(String key, String v, int min, int max) {
        try {
            int i = Integer.parseInt(v);
            if (i < min || i > max) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.AI_CONFIG_INVALID, "%s phải là số nguyên trong [%d, %d]".formatted(key, min, max));
        }
    }

    private static double parseDouble(String v, double fallback) {
        try {
            return Double.parseDouble(v);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int parseInt(String v, int fallback) {
        try {
            return Integer.parseInt(v);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /**
     * Cấu hình hiệu lực tại một thời điểm. {@code topN} không vượt quá {@code topK}.
     */
    public record Snapshot(
            String model,
            Map<AiTask, String> taskModels,
            double temperature,
            int topK,
            int topN,
            double scoreThreshold,
            String sttLanguage,
            String disclaimer,
            List<String> blockedTopics,
            Map<String, String> raw
    ) {
        public int effectiveTopN() {
            return Math.min(topN, topK);
        }

        /** Model dùng cho một tác vụ. */
        public String model(AiTask task) {
            return taskModels.getOrDefault(task, model);
        }
    }
}
