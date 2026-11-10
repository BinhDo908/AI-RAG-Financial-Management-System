package com.fintech.chatbot.dto.response;


/**
 * UC-D05 — Quyền dữ liệu AI hiện tại.
 */
public record AiDataPermissionResponse(
        boolean allowTransactions,
        boolean allowBudgets,
        boolean allowDebts,
        boolean allowGoals,
        boolean allowProfile
) {
}
