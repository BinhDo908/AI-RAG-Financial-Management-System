package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * UC-A03 AF-3 — Gộp danh mục nguồn vào danh mục đích.
 */
public record MergeCategoryRequest(
        @NotNull(message = "Vui lòng chọn danh mục đích") Long targetCategoryId
) {
}
