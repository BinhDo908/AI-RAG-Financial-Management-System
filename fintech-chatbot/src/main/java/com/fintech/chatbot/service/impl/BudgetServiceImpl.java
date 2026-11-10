package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.util.Months;
import com.fintech.chatbot.dao.BudgetDao;
import com.fintech.chatbot.dto.request.BudgetRequest;
import com.fintech.chatbot.dto.request.BudgetUpdateRequest;
import com.fintech.chatbot.dto.response.BudgetResponse;
import com.fintech.chatbot.entity.Budget;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.enums.CategoryType;
import com.fintech.chatbot.service.BudgetService;
import com.fintech.chatbot.service.support.BudgetAlertService;
import com.fintech.chatbot.service.support.BudgetCalculator;
import com.fintech.chatbot.service.support.BudgetUsage;
import com.fintech.chatbot.service.support.FinanceLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetDao budgetDao;
    private final FinanceLookup lookup;
    private final BudgetCalculator calculator;
    private final BudgetAlertService alertService;

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId, String month) {
        Months.parse(month);
        return budgetDao.findByMonth(userId, month).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse get(Long userId, Long budgetId) {
        return toResponse(load(userId, budgetId));
    }

    @Override
    @Transactional
    public BudgetResponse create(Long userId, BudgetRequest request) {
        Months.parse(request.month());
        Category category = lookup.category(userId, request.categoryId());
        if (category.getType() != CategoryType.EXPENSE) {
            throw new BusinessException(ErrorCode.BUDGET_CATEGORY_INVALID);
        }
        if (budgetDao.findByCategoryAndMonth(userId, category.getId(), request.month()).isPresent()) {
            throw new BusinessException(ErrorCode.BUDGET_DUPLICATE);
        }
        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.assignCategory(category);
        budget.setBudgetMonth(request.month());
        budget.setLimitAmount(request.limitAmount());
        budget.setWarnThreshold(request.warnThreshold() == null
                ? Budget.DEFAULT_WARN_THRESHOLD : request.warnThreshold());
        budgetDao.save(budget);
        alertService.check(budget);
        return toResponse(budget);
    }

    @Override
    @Transactional
    public BudgetResponse update(Long userId, Long budgetId, BudgetUpdateRequest request) {
        Budget budget = load(userId, budgetId);
        budget.setLimitAmount(request.limitAmount());
        if (request.warnThreshold() != null) {
            budget.setWarnThreshold(request.warnThreshold());
        }
        alertService.check(budget);
        return toResponse(budget);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long budgetId) {
        budgetDao.delete(load(userId, budgetId));
    }

    @Override
    @Transactional
    public List<BudgetResponse> copyFromPreviousMonth(Long userId, String month) {
        String previous = Months.parse(month).minusMonths(1).toString();
        List<BudgetResponse> created = new ArrayList<>();
        for (Budget old : budgetDao.findByMonth(userId, previous)) {
            if (budgetDao.findByCategoryAndMonth(userId, old.getCategoryId(), month).isPresent()) {
                continue;
            }
            Budget copy = new Budget();
            copy.setUserId(userId);
            copy.assignCategory(old.getCategory());
            copy.setBudgetMonth(month);
            copy.setLimitAmount(old.getLimitAmount());
            copy.setWarnThreshold(old.getWarnThreshold());
            budgetDao.save(copy);
            created.add(toResponse(copy));
        }
        return created;
    }

    private Budget load(Long userId, Long budgetId) {
        return budgetDao.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> BusinessException.notFound("ngân sách"));
    }

    private BudgetResponse toResponse(Budget b) {
        BudgetUsage usage = calculator.usage(b);
        return new BudgetResponse(b.getId(), b.getCategoryId(), b.getCategory().getName(), b.getBudgetMonth(),
                b.getLimitAmount(), b.getWarnThreshold(), usage.spent(), usage.remaining(),
                usage.percentUsed(), usage.status());
    }
}
