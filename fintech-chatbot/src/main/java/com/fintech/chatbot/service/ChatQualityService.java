package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.EvaluationRequest;
import com.fintech.chatbot.dto.response.ChatQualityResponses;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC-C03 — Dashboard chất lượng chatbot và bộ đánh giá.
 */
public interface ChatQualityService {

    /** Mặc định 30 ngày gần nhất. */
    ChatQualityResponses.Dashboard dashboard(LocalDateTime from, LocalDateTime to);

    /**
     * AF-1: chạy từng câu qua pipeline RAG (không lưu thành hội thoại) rồi tính
     * Hit@k (tài liệu đúng nằm trong top_k), tỷ lệ có trích dẫn, tỷ lệ đúng (LLM chấm so với đáp án mẫu).
     */
    ChatQualityResponses.EvalRunDetail evaluate(Long adminId, EvaluationRequest request);

    List<ChatQualityResponses.EvalRunView> evaluations();

    ChatQualityResponses.EvalRunDetail evaluation(Long id);
}
