package com.fintech.chatbot.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * UC-C03 — Dashboard chất lượng chatbot (số liệu ẩn danh, tổng hợp).
 */
public final class ChatQualityResponses {

    private ChatQualityResponses() {
    }

    public record Dashboard(
            LocalDateTime from,
            LocalDateTime to,
            long activeUsers,
            long conversations,
            long userMessages,
            Map<String, Long> intentDistribution,
            double voiceRatio,
            long thumbsUp,
            long thumbsDown,
            double thumbsUpRatio,
            long knowledgeAnswers,
            long noSourceAnswers,
            double noSourceRate,
            List<String> unansweredQuestions,
            Double averageLatencyMs,
            EvalRunView latestEvaluation
    ) {
    }

    public record EvalRunView(Long id, String datasetName, int totalQuestions, double hitRate, double citationRate,
                              double accuracy, JsonNode configSnapshot, LocalDateTime createdAt) {
    }

    public record EvalRunDetail(EvalRunView run, JsonNode details) {
    }
}
