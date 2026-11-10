package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.BudgetRequest;
import com.fintech.chatbot.dto.request.BudgetUpdateRequest;
import com.fintech.chatbot.dto.response.BudgetResponse;

import java.util.List;

/**
 * UC-A04 — Quản lý ngân sách theo danh mục chi và tháng.
 */
public interface BudgetService {

    /** Ngân sách của tháng {@code yyyy-MM} kèm đã chi / còn lại / % / trạng thái. */
    List<BudgetResponse> list(Long userId, String month);

    BudgetResponse get(Long userId, Long budgetId);

    BudgetResponse create(Long userId, BudgetRequest request);

    BudgetResponse update(Long userId, Long budgetId, BudgetUpdateRequest request);

    void delete(Long userId, Long budgetId);

    /** AF-1: sao chép ngân sách tháng trước sang {@code month}, bỏ qua danh mục đã có ngân sách. */
    List<BudgetResponse> copyFromPreviousMonth(Long userId, String month);
}
