package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * UC-B05 (1) — Lãi kép có góp định kỳ.
 */
public record CompoundInterestRequest(
        @NotNull(message = "Vui lòng nhập số tiền ban đầu") @PositiveOrZero @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal principal,
        @NotNull(message = "Vui lòng nhập số tiền góp mỗi kỳ") @PositiveOrZero @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal contribution,
        @NotNull(message = "Vui lòng nhập lãi suất") @DecimalMin(value = "0", message = "Lãi suất không được âm") @DecimalMax(value = "100", message = "Lãi suất tối đa 100%/năm") BigDecimal annualRatePercent,
        @Min(value = 1, message = "Số kỳ ghép lãi mỗi năm từ 1") @Max(value = 365) int periodsPerYear,
        @Min(value = 1, message = "Số năm từ 1") @Max(value = 50, message = "Số năm tối đa 50") int years
) {
}
