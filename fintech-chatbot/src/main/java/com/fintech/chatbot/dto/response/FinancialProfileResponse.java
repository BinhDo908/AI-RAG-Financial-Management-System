package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.FinancialGoal;
import com.fintech.chatbot.entity.enums.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Hồ sơ tài chính.
 */
public record FinancialProfileResponse(
        BigDecimal monthlyIncome,
        int dependents,
        List<FinancialGoal> goals,
        Integer riskScore,
        RiskLevel riskLevel,
        String riskDescription,
        LocalDateTime updatedAt
) {
}
