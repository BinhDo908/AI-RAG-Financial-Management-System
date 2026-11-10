package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.WalletType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ví tiền.
 */
public record WalletResponse(
        Long id,
        String name,
        WalletType type,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        boolean defaultWallet,
        boolean archived,
        LocalDateTime createdAt
) {
}
