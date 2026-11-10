package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * UC-A04 AF-3 — Sửa hạn mức / ngưỡng cảnh báo.
 */
public record BudgetUpdateRequest(
        @NotNull(message = "Vui lòng nhập hạn mức") @Positive(message = "Hạn mức phải lớn hơn 0") @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal limitAmount,
        @Min(value = 50, message = "Ngưỡng cảnh báo từ 50%") @Max(value = 95, message = "Ngưỡng cảnh báo tối đa 95%") Integer warnThreshold
) {
}
