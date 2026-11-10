package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.FinancialProfileRequest;
import com.fintech.chatbot.dto.request.RiskAnswersRequest;
import com.fintech.chatbot.dto.response.FinancialProfileResponse;
import com.fintech.chatbot.dto.response.RiskQuestionResponse;

import java.util.List;

/**
 * UC-B04 — Hồ sơ tài chính và bảng hỏi khẩu vị rủi ro.
 */
public interface FinancialProfileService {

    /** Hồ sơ hiện tại; user chưa điền thì trả hồ sơ rỗng (AF-1 bỏ qua). */
    FinancialProfileResponse get(Long userId);

    FinancialProfileResponse update(Long userId, FinancialProfileRequest request);

    List<RiskQuestionResponse> riskQuestions();

    /** Bước 3: tổng điểm 5–8 = LOW, 9–12 = MEDIUM, 13–15 = HIGH. */
    FinancialProfileResponse submitRiskAnswers(Long userId, RiskAnswersRequest request);
}
