package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.DebtStatus;
import com.fintech.chatbot.entity.enums.DebtType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Khoản nợ.
 */
public record DebtResponse(
        Long id,
        DebtType type,
        String counterparty,
        BigDecimal principal,
        BigDecimal remaining,
        BigDecimal paidAmount,
        LocalDate startDate,
        LocalDate dueDate,
        String note,
        DebtStatus status,
        boolean overdue
) {
}
