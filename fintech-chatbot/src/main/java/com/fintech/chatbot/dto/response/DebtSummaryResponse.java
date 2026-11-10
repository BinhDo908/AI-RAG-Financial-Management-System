package com.fintech.chatbot.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * UC-A06 AF-1 — Tổng đang nợ, tổng đang cho vay, khoản sắp đến hạn (7 ngày).
 */
public record DebtSummaryResponse(
        BigDecimal totalBorrowing,
        BigDecimal totalLending,
        List<DebtResponse> dueSoon
) {
}
