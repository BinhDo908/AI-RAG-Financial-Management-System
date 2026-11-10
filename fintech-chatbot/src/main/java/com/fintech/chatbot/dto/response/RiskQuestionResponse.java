package com.fintech.chatbot.dto.response;

import java.util.List;

/**
 * Một câu hỏi trong bảng hỏi khẩu vị rủi ro; lựa chọn thứ k (bắt đầu từ 1) được k điểm.
 */
public record RiskQuestionResponse(
        int number,
        String question,
        List<String> options
) {
}
