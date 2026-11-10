package com.fintech.chatbot.dto.request;

import com.fintech.chatbot.entity.enums.InputMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * UC-A11 bước 6 / AF-2 — User xác nhận (và có thể đã sửa) các thẻ giao dịch nháp → lưu (BR-04).
 */
public record ConfirmTransactionsRequest(
        Long conversationId,
        InputMode inputMode,
        @NotEmpty(message = "Không có giao dịch nào để lưu") @Size(max = 20) List<@Valid TransactionRequest> transactions
) {
}
