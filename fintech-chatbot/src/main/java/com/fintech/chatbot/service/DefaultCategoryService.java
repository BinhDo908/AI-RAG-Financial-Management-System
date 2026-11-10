package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.CategoryRequest;
import com.fintech.chatbot.dto.response.CategoryResponse;
import com.fintech.chatbot.entity.enums.CategoryType;

import java.util.List;

/**
 * UC-C06 — Admin quản lý bộ danh mục mặc định cấp cho mọi user.
 */
public interface DefaultCategoryService {

    List<CategoryResponse> list(CategoryType type, boolean includeHidden);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    CategoryResponse setHidden(Long id, boolean hidden);

    /** Chỉ xóa được danh mục chưa từng được dùng (BR-10). */
    void delete(Long id);
}
