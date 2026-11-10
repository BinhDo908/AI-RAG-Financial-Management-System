package com.fintech.chatbot.dto.request;


/**
 * UC-D05 — Bật / tắt từng nhóm dữ liệu AI được dùng.
 */
public record AiDataPermissionRequest(
        boolean allowTransactions,
        boolean allowBudgets,
        boolean allowDebts,
        boolean allowGoals,
        boolean allowProfile
) {
}
