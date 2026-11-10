package com.fintech.chatbot.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * UC-C03 AF-1 — Bộ câu hỏi đánh giá (ví dụ SRC-22: ≥ 50 câu tiếng Việt có đáp án + nguồn).
 */
public record EvaluationRequest(
        @NotBlank @Size(max = 200) String datasetName,
        @NotEmpty @Size(max = 100, message = "Tối đa 100 câu mỗi lần chạy") List<@Valid Item> items
) {

    /**
     * @param expectedSourceId   mã nguồn (SRC-xx) chứa câu trả lời — dùng tính Hit@k
     * @param expectedDocumentId hoặc id tài liệu cụ thể (ưu tiên nếu có)
     */
    public record Item(
            @NotBlank @Size(max = 1000) String question,
            @NotBlank @Size(max = 2000) String expectedAnswer,
            String expectedSourceId,
            Long expectedDocumentId
    ) {
    }
}
