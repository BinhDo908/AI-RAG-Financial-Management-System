package com.fintech.chatbot.entity;

import com.fintech.chatbot.entity.enums.GoalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * E-06 — Mục tiêu tiết kiệm (UC-A05).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "saving_goals")
public class SavingGoal extends UserOwnedEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GoalStatus status = GoalStatus.IN_PROGRESS;
}
