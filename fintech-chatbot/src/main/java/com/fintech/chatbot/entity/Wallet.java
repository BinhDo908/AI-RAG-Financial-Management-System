package com.fintech.chatbot.entity;

import com.fintech.chatbot.entity.enums.WalletType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * E-02 — Ví tiền (UC-A01). Số dư hiện tại được tính lại từ giao dịch (BR-07).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "wallets")
public class Wallet extends UserOwnedEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private WalletType type;

    @Column(name = "initial_balance", nullable = false, precision = 18, scale = 0)
    private BigDecimal initialBalance = BigDecimal.ZERO;

    @Column(name = "current_balance", nullable = false, precision = 18, scale = 0)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "is_default", nullable = false)
    private boolean defaultWallet;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;
}
