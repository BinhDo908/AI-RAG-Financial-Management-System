package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Chuyển tiền giữa một ví và mục tiêu / khoản nợ (nạp, rút, trả nợ, thu nợ).
 */
public record WalletAmountRequest(
        @NotNull(message = "Vui lòng chọn ví") Long walletId,
        @NotNull(message = "Vui lòng nhập số tiền") @Positive(message = "Số tiền phải lớn hơn 0") @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal amount,
        LocalDate date,
        @Size(max = 500) String note
) {
}
