package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Giao dịch.
 */
public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        LocalDate txnDate,
        String note,
        TransactionSource source,
        Long walletId,
        String walletName,
        Long toWalletId,
        String toWalletName,
        Long categoryId,
        String categoryName,
        Long debtId,
        Long goalId,
        LocalDateTime createdAt
) {
}
