package com.fintech.chatbot.dto.response;

import java.math.BigDecimal;

/**
 * Kết quả lưu giao dịch, kèm số dư ví sau khi lưu và cảnh báo số dư âm (UC-A02 AF-3).
 */
public record TransactionResult(
        TransactionResponse transaction,
        BigDecimal walletBalance,
        boolean negativeBalance
) {
}
