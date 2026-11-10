package com.fintech.chatbot.dto.response;

import java.math.BigDecimal;

/**
 * Ngân sách kèm tình hình chi tiêu thực tế trong tháng.
 */
public record BudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String month,
        BigDecimal limitAmount,
        int warnThreshold,
        BigDecimal spent,
        BigDecimal remaining,
        int percentUsed,
        Status status
) {

    /** OK: dưới ngưỡng cảnh báo; WARNING: từ ngưỡng đến dưới 100%; OVER: từ 100%. */
    public enum Status {
        OK, WARNING, OVER
    }
}
