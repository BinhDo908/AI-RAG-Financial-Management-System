package com.fintech.chatbot.service.support;

import com.fintech.chatbot.dto.response.BudgetResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mức sử dụng một ngân sách trong tháng.
 */
public record BudgetUsage(BigDecimal limit, BigDecimal spent, int warnThreshold) {

    public BigDecimal remaining() {
        return limit.subtract(spent);
    }

    /** Phần trăm đã dùng, làm tròn xuống (82,7% → 82). */
    public int percentUsed() {
        if (limit.signum() <= 0) {
            return 0;
        }
        return spent.multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.DOWN).intValue();
    }

    public BudgetResponse.Status status() {
        if (spent.compareTo(limit) >= 0) {
            return BudgetResponse.Status.OVER;
        }
        // So sánh chính xác thay vì dùng percentUsed() đã làm tròn
        BigDecimal warnAt = limit.multiply(BigDecimal.valueOf(warnThreshold)).divide(BigDecimal.valueOf(100));
        return spent.compareTo(warnAt) >= 0 ? BudgetResponse.Status.WARNING : BudgetResponse.Status.OK;
    }
}
