package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.common.response.PageResponse;

import java.math.BigDecimal;

/**
 * UC-A08 — Kết quả lọc giao dịch kèm tổng thu / chi của toàn bộ kết quả.
 */
public record TransactionSearchResponse(
        PageResponse<TransactionResponse> page,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal net
) {
}
