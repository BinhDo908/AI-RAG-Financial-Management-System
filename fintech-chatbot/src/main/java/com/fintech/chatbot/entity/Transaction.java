package com.fintech.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fintech.chatbot.entity.enums.TransactionSource;
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
 * E-04 — Giao dịch thu / chi / chuyển tiền / điều chỉnh (UC-A02).
 *
 * <p>Quy ước: quan hệ {@code @ManyToOne} dùng để ghi; cột {@code *_id} chỉ đọc đi kèm
 * dùng cho truy vấn và xuất dữ liệu mà không phải nạp entity liên quan.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction extends UserOwnedEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "wallet_id", insertable = false, updatable = false)
    private Long walletId;

    /** Chỉ dùng với TRANSFER: ví nhận. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @Column(name = "to_wallet_id", insertable = false, updatable = false)
    private Long toWalletId;

    /** Bắt buộc với INCOME / EXPENSE; null với TRANSFER / ADJUST. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "category_id", insertable = false, updatable = false)
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    /** Dương với INCOME / EXPENSE / TRANSFER (chiều tiền do {@link #type} quyết định); ADJUST mang dấu (V3). */
    @Column(name = "amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal amount;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(name = "note", length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private TransactionSource source = TransactionSource.MANUAL;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id")
    private Debt debt;

    @Column(name = "debt_id", insertable = false, updatable = false)
    private Long debtId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id")
    private SavingGoal goal;

    @Column(name = "goal_id", insertable = false, updatable = false)
    private Long goalId;

    // Gán quan hệ đồng thời cập nhật cột id chỉ đọc, để entity nhất quán ngay cả trước khi flush.

    public void assignWallet(Wallet value) {
        wallet = value;
        walletId = value == null ? null : value.getId();
    }

    public void assignToWallet(Wallet value) {
        toWallet = value;
        toWalletId = value == null ? null : value.getId();
    }

    public void assignCategory(Category value) {
        category = value;
        categoryId = value == null ? null : value.getId();
    }

    public void assignDebt(Debt value) {
        debt = value;
        debtId = value == null ? null : value.getId();
    }

    public void assignGoal(SavingGoal value) {
        goal = value;
        goalId = value == null ? null : value.getId();
    }

    /** Giao dịch sinh ra từ khoản nợ / mục tiêu tiết kiệm chỉ được sửa qua chức năng tương ứng. */
    public boolean isLinked() {
        return debtId != null || goalId != null;
    }
}
