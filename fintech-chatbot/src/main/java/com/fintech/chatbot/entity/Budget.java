package com.fintech.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * E-05 — Ngân sách theo danh mục và tháng (UC-A04). Ngưỡng cảnh báo mặc định 80% (BR-11).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "budgets")
public class Budget extends UserOwnedEntity {

    public static final int DEFAULT_WARN_THRESHOLD = 80;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "category_id", insertable = false, updatable = false)
    private Long categoryId;

    /** Định dạng {@code yyyy-MM}, ví dụ "2026-10". */
    @Column(name = "budget_month", nullable = false, length = 7)
    private String budgetMonth;

    @Column(name = "limit_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal limitAmount;

    /** Phần trăm, trong khoảng 50–95 (BR-11). */
    @Column(name = "warn_threshold", nullable = false)
    private int warnThreshold = DEFAULT_WARN_THRESHOLD;

    public void assignCategory(Category value) {
        category = value;
        categoryId = value == null ? null : value.getId();
    }
}
