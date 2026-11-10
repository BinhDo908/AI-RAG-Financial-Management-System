package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bộ lọc giao dịch (UC-A08). Mọi trường đều tùy chọn; null = không lọc.
 * {@code walletId} khớp cả ví nguồn lẫn ví đích của giao dịch chuyển tiền.
 */
public record TransactionFilter(
        LocalDate from,
        LocalDate to,
        Long walletId,
        Long categoryId,
        TransactionType type,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        TransactionSource source,
        String keyword
) {

    public static TransactionFilter between(LocalDate from, LocalDate to) {
        return new TransactionFilter(from, to, null, null, null, null, null, null, null);
    }
}
