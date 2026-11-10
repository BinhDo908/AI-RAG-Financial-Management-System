package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.RecurrenceFrequency;
import com.fintech.chatbot.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Quy tắc giao dịch định kỳ.
 */
public record RecurringRuleResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        Long walletId,
        String walletName,
        Long categoryId,
        String categoryName,
        String note,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate nextRunDate,
        boolean autoCreate,
        boolean active
) {
}
