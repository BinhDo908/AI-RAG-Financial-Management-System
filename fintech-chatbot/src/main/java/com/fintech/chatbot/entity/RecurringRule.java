package com.fintech.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fintech.chatbot.entity.enums.RecurrenceFrequency;
import com.fintech.chatbot.entity.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * E-08 — Quy tắc giao dịch định kỳ (UC-A07), được UC-D04 xử lý hằng ngày.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "recurring_rules")
public class RecurringRule extends UserOwnedEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "wallet_id", insertable = false, updatable = false)
    private Long walletId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "category_id", insertable = false, updatable = false)
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal amount;

    @Column(name = "note", length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    /** true = tự tạo giao dịch; false = chỉ nhắc để user xác nhận. */
    @Column(name = "auto_create", nullable = false)
    private boolean autoCreate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public void assignWallet(Wallet value) {
        wallet = value;
        walletId = value == null ? null : value.getId();
    }

    public void assignCategory(Category value) {
        category = value;
        categoryId = value == null ? null : value.getId();
    }
}
