package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * UC-A06 — Sửa thông tin khoản nợ (không sửa số tiền).
 */
public record DebtUpdateRequest(
        @NotBlank(message = "Vui lòng nhập người liên quan") @Size(max = 100) String counterparty,
        LocalDate dueDate,
        @Size(max = 500) String note
) {
}
