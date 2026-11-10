package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * UC-C01 bước 3 — Metadata bắt buộc của tài liệu nguồn mở (BR-03, NFR-09).
 */
public record DocumentMetadataRequest(
        @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 300) String title,
        @NotBlank(message = "Vui lòng chọn nguồn") @Pattern(regexp = "SRC-\\d{2}", message = "Mã nguồn dạng SRC-01 (USE_CASES.md §4)") String sourceId,
        @Size(max = 1000) String url,
        @NotBlank(message = "Vui lòng nhập giấy phép") @Size(max = 200) String license,
        @NotBlank @Pattern(regexp = "vi|en", message = "Ngôn ngữ: vi hoặc en") String language,
        LocalDate accessedAt
) {
}
