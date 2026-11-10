package com.fintech.chatbot.dto.request;

import com.fintech.chatbot.entity.enums.DebtType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UC-A06 — Ghi khoản đi vay / cho vay.
 */
public record DebtCreateRequest(
        @NotNull(message = "Vui lòng chọn Đi vay hoặc Cho vay") DebtType type,
        @NotBlank(message = "Vui lòng nhập người liên quan") @Size(max = 100) String counterparty,
        @NotNull(message = "Vui lòng nhập số tiền") @Positive(message = "Số tiền phải lớn hơn 0") @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal principal,
        @NotNull(message = "Vui lòng chọn ví") Long walletId,
        LocalDate startDate,
        LocalDate dueDate,
        @Size(max = 500) String note
) {
}
