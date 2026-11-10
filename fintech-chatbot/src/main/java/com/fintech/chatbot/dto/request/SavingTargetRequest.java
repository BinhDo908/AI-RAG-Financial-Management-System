package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * UC-B05 (5) — Số tiền cần góp mỗi tháng để đạt mục tiêu.
 */
public record SavingTargetRequest(
        @NotNull(message = "Vui lòng nhập số tiền mục tiêu") @Positive @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal targetAmount,
        @PositiveOrZero @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal currentAmount,
        @NotNull(message = "Vui lòng nhập lãi suất") @DecimalMin(value = "0", message = "Lãi suất không được âm") @DecimalMax(value = "100", message = "Lãi suất tối đa 100%/năm") BigDecimal annualRatePercent,
        @Min(value = 1, message = "Số tháng từ 1") @Max(value = 600, message = "Số tháng tối đa 600") int months
) {
}
