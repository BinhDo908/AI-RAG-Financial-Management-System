package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UC-A01 AF-1 / BR-07 — Điều chỉnh số dư bằng giao dịch ADJUST.
 */
public record AdjustBalanceRequest(
        @NotNull(message = "Vui lòng nhập số dư mới") @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal newBalance,
        LocalDate date,
        @Size(max = 500) String note
) {
}
