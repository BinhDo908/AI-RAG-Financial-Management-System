package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * UC-B04 bước 2 — 5 câu trả lời bảng hỏi khẩu vị rủi ro, mỗi câu 1–3 điểm.
 */
public record RiskAnswersRequest(
        @NotNull @Size(min = 5, max = 5, message = "Cần trả lời đủ 5 câu") List<@NotNull @Min(1) @Max(3) Integer> answers
) {
}
