package com.fintech.chatbot.service.support;

import com.fintech.chatbot.common.util.Months;
import com.fintech.chatbot.dao.CategoryDao;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.entity.Budget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Tính số đã chi của một ngân sách: tổng chi trong tháng của danh mục và các danh mục con trực tiếp.
 */
@Component
@RequiredArgsConstructor
public class BudgetCalculator {

    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;

    public BudgetUsage usage(Budget budget) {
        YearMonth month = Months.parse(budget.getBudgetMonth());
        List<Long> categoryIds = new ArrayList<>();
        categoryIds.add(budget.getCategoryId());
        categoryIds.addAll(categoryDao.findChildIds(budget.getCategoryId()));
        BigDecimal spent = transactionDao.sumExpense(
                budget.getUserId(), categoryIds, month.atDay(1), month.atEndOfMonth());
        return new BudgetUsage(budget.getLimitAmount(), spent, budget.getWarnThreshold());
    }
}
