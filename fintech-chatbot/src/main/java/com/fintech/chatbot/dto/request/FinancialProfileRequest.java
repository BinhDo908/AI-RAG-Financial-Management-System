package com.fintech.chatbot.dto.request;

import com.fintech.chatbot.entity.enums.FinancialGoal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * UC-B04 — Thông tin nền cho tư vấn.
 */
public record FinancialProfileRequest(
        @PositiveOrZero(message = "Thu nhập không được âm") @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal monthlyIncome,
        @Min(value = 0, message = "Số người phụ thuộc không được âm") @Max(value = 20) int dependents,
        @Size(max = 6) List<FinancialGoal> goals
) {
}
