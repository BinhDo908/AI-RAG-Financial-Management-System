package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.CategoryRequest;
import com.fintech.chatbot.dto.request.MergeCategoryRequest;
import com.fintech.chatbot.dto.response.CategoryResponse;
import com.fintech.chatbot.entity.enums.CategoryType;

import java.util.List;

/**
 * UC-A03 — Danh mục của user. Danh mục mặc định chỉ đọc với user (Admin quản lý ở UC-C06).
 */
public interface UserCategoryService {

    /** Danh mục mặc định chưa ẩn + danh mục riêng (kèm danh mục riêng đã ẩn nếu {@code includeHidden}). */
    List<CategoryResponse> list(Long userId, CategoryType type, boolean includeHidden);

    CategoryResponse create(Long userId, CategoryRequest request);

    CategoryResponse update(Long userId, Long categoryId, CategoryRequest request);

    CategoryResponse setHidden(Long userId, Long categoryId, boolean hidden);

    /** Chỉ xóa được danh mục riêng chưa được dùng (BR-10). */
    void delete(Long userId, Long categoryId);

    /** AF-3: chuyển giao dịch, ngân sách, quy tắc định kỳ sang danh mục đích rồi xóa danh mục nguồn. */
    CategoryResponse merge(Long userId, Long sourceCategoryId, MergeCategoryRequest request);
}
