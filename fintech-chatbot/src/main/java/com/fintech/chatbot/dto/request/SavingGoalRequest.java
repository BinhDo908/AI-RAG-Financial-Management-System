package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UC-A05 — Thêm / sửa mục tiêu tiết kiệm.
 */
public record SavingGoalRequest(
        @NotBlank(message = "Tên mục tiêu không được để trống") @Size(max = 100) String name,
        @NotNull(message = "Vui lòng nhập số tiền mục tiêu") @Positive(message = "Số tiền mục tiêu phải lớn hơn 0") @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal targetAmount,
        LocalDate deadline
) {
}
