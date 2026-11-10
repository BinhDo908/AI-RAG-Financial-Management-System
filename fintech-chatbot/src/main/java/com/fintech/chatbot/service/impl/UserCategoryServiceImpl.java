package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.BudgetDao;
import com.fintech.chatbot.dao.CategoryDao;
import com.fintech.chatbot.dao.RecurringRuleDao;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dto.request.CategoryRequest;
import com.fintech.chatbot.dto.request.MergeCategoryRequest;
import com.fintech.chatbot.dto.response.CategoryResponse;
import com.fintech.chatbot.entity.Budget;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.enums.CategoryType;
import com.fintech.chatbot.mapper.CategoryMapper;
import com.fintech.chatbot.service.UserCategoryService;
import com.fintech.chatbot.service.support.FinanceLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCategoryServiceImpl implements UserCategoryService {

    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;
    private final RecurringRuleDao recurringRuleDao;
    private final FinanceLookup lookup;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId, CategoryType type, boolean includeHidden) {
        List<Category> result = new ArrayList<>(categoryDao.findVisibleForUser(userId, type));
        if (includeHidden) {
            categoryDao.findOwnedByUser(userId).stream()
                    .filter(Category::isHidden)
                    .filter(c -> type == null || c.getType() == type)
                    .forEach(result::add);
        }
        return categoryMapper.toResponses(result);
    }

    @Override
    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        checkNameAvailable(userId, request, null);
        Category category = new Category();
        category.setUserId(userId);
        apply(userId, category, request);
        categoryDao.save(category);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long userId, Long categoryId, CategoryRequest request) {
        Category category = loadOwned(userId, categoryId);
        if (category.getType() != request.type() && categoryDao.isInUse(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE, "Không thể đổi loại của danh mục đã được sử dụng");
        }
        checkNameAvailable(userId, request, categoryId);
        apply(userId, category, request);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse setHidden(Long userId, Long categoryId, boolean hidden) {
        Category category = loadOwned(userId, categoryId);
        category.setHidden(hidden);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = loadOwned(userId, categoryId);
        if (categoryDao.isInUse(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }
        categoryDao.delete(category);
    }

    @Override
    @Transactional
    public CategoryResponse merge(Long userId, Long sourceCategoryId, MergeCategoryRequest request) {
        Category source = loadOwned(userId, sourceCategoryId);
        Category target = lookup.category(userId, request.targetCategoryId());
        if (source.getId().equals(target.getId()) || source.getType() != target.getType()) {
            throw new BusinessException(ErrorCode.CATEGORY_MERGE_INVALID);
        }

        transactionDao.reassignCategory(userId, source.getId(), target.getId());
        recurringRuleDao.reassignCategory(userId, source.getId(), target.getId());

        // Ngân sách: tháng nào danh mục đích đã có ngân sách thì cộng hạn mức, ngược lại chuyển sang
        for (Budget budget : budgetDao.findByCategory(userId, source.getId())) {
            budgetDao.findByCategoryAndMonth(userId, target.getId(), budget.getBudgetMonth())
                    .ifPresentOrElse(existing -> {
                        existing.setLimitAmount(existing.getLimitAmount().add(budget.getLimitAmount()));
                        budgetDao.delete(budget);
                    }, () -> budget.assignCategory(target));
        }

        // Danh mục con của nguồn trở thành danh mục cấp 1
        for (Category child : categoryDao.findOwnedByUser(userId)) {
            if (source.getId().equals(child.getParentId())) {
                child.assignParent(null);
            }
        }

        categoryDao.flush();
        categoryDao.delete(source);
        return categoryMapper.toResponse(target);
    }

    private void checkNameAvailable(Long userId, CategoryRequest request, Long excludeId) {
        if (categoryDao.existsByName(userId, request.type(), request.name(), excludeId)
                || categoryDao.existsByName(null, request.type(), request.name(), null)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_DUPLICATE);
        }
    }

    private void apply(Long userId, Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setIcon(request.icon());
        if (request.parentId() == null) {
            category.assignParent(null);
            return;
        }
        Category parent = lookup.category(userId, request.parentId());
        boolean invalid = parent.getId().equals(category.getId())
                || parent.getType() != request.type()
                || parent.getParentId() != null; // chỉ hỗ trợ 2 cấp
        if (invalid) {
            throw new BusinessException(ErrorCode.CATEGORY_PARENT_INVALID);
        }
        category.assignParent(parent);
    }

    private Category loadOwned(Long userId, Long categoryId) {
        Category category = lookup.category(userId, categoryId);
        if (category.isDefaultCategory()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_EDITABLE);
        }
        return category;
    }
}
