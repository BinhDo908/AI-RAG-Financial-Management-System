package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mục tiêu tiết kiệm và tiến độ (UC-A05 bước 2).
 */
public record SavingGoalResponse(
        Long id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        int percent,
        LocalDate deadline,
        GoalStatus status,
        BigDecimal monthlyNeeded,
        boolean overdue
) {
}
