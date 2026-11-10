package com.fintech.chatbot.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Danh sách ví và tổng số dư các ví đang dùng (UC-A01 bước 1).
 */
public record WalletListResponse(
        List<WalletResponse> wallets,
        BigDecimal totalBalance
) {
}
