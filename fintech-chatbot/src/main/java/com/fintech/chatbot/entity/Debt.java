package com.fintech.chatbot.entity;

import com.fintech.chatbot.entity.enums.DebtStatus;
import com.fintech.chatbot.entity.enums.DebtType;
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
 * E-07 — Khoản đi vay / cho vay (UC-A06).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "debts")
public class Debt extends UserOwnedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DebtType type;

    @Column(name = "counterparty", nullable = false, length = 100)
    private String counterparty;

    @Column(name = "principal", nullable = false, precision = 18, scale = 0)
    private BigDecimal principal;

    @Column(name = "remaining", nullable = false, precision = 18, scale = 0)
    private BigDecimal remaining;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "note", length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DebtStatus status = DebtStatus.OPEN;
}
