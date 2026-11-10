package com.fintech.chatbot.dto.response;

import java.util.List;

/**
 * UC-A11 bước 7 — Giao dịch đã lưu và lời nhắn của chatbot (kèm ngân sách còn lại nếu có).
 */
public record ConfirmTransactionsResponse(List<TransactionResult> results, ChatMessageView reply) {
}
