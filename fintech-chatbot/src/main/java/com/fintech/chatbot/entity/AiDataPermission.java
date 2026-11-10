package com.fintech.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E-17 — Nhóm dữ liệu user cho phép AI sử dụng (UC-D05, BR-05). Mặc định bật tất cả.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_data_permissions")
public class AiDataPermission extends UserOwnedEntity {

    @Column(name = "allow_transactions", nullable = false)
    private boolean allowTransactions = true;

    @Column(name = "allow_budgets", nullable = false)
    private boolean allowBudgets = true;

    @Column(name = "allow_debts", nullable = false)
    private boolean allowDebts = true;

    @Column(name = "allow_goals", nullable = false)
    private boolean allowGoals = true;

    @Column(name = "allow_profile", nullable = false)
    private boolean allowProfile = true;
}
