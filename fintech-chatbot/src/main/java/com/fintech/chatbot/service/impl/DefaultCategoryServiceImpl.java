package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.CategoryDao;
import com.fintech.chatbot.dto.request.CategoryRequest;
import com.fintech.chatbot.dto.response.CategoryResponse;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.enums.CategoryType;
import com.fintech.chatbot.mapper.CategoryMapper;
import com.fintech.chatbot.service.DefaultCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DefaultCategoryServiceImpl implements DefaultCategoryService {

    private final CategoryDao categoryDao;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> list(CategoryType type, boolean includeHidden) {
        return categoryMapper.toResponses(categoryDao.findDefaults(type, includeHidden));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryDao.existsByName(null, request.type(), request.name(), null)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_DUPLICATE);
        }
        Category category = new Category();
        apply(category, request);
        categoryDao.save(category);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = loadDefault(id);
        if (category.getType() != request.type() && categoryDao.isInUse(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE, "Không thể đổi loại của danh mục đã được sử dụng");
        }
        if (categoryDao.existsByName(null, request.type(), request.name(), id)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_DUPLICATE);
        }
        apply(category, request);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse setHidden(Long id, boolean hidden) {
        Category category = loadDefault(id);
        category.setHidden(hidden);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = loadDefault(id);
        if (categoryDao.isInUse(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }
        categoryDao.delete(category);
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setIcon(request.icon());
        if (request.parentId() == null) {
            category.assignParent(null);
            return;
        }
        Category parent = loadDefault(request.parentId());
        boolean invalid = Objects.equals(parent.getId(), category.getId())
                || parent.getType() != request.type()
                || parent.getParentId() != null; // chỉ hỗ trợ 2 cấp
        if (invalid) {
            throw new BusinessException(ErrorCode.CATEGORY_PARENT_INVALID);
        }
        category.assignParent(parent);
    }

    private Category loadDefault(Long id) {
        return categoryDao.findDefaultById(id).orElseThrow(() -> BusinessException.notFound("danh mục mặc định"));
    }
}
