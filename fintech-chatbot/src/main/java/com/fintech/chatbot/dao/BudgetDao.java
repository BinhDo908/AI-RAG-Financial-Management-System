package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Budget;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * DAO cho E-05 Budget (UC-A04, B06).
 */
public interface BudgetDao extends UserOwnedDao<Budget, Long> {

    List<Budget> findByMonth(Long userId, String month);

    Optional<Budget> findByCategoryAndMonth(Long userId, Long categoryId, String month);

    /** Ngân sách trong tháng của bất kỳ danh mục nào trong danh sách (dùng khi kiểm tra cảnh báo). */
    List<Budget> findByCategoriesAndMonth(Long userId, Collection<Long> categoryIds, String month);

    List<Budget> findByCategory(Long userId, Long categoryId);
}
