package com.fintech.chatbot.dto.request;

import com.fintech.chatbot.entity.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-C06 / UC-A03 — Tạo / sửa danh mục.
 */
public record CategoryRequest(
        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
        String name,

        @NotNull(message = "Vui lòng chọn loại danh mục")
        CategoryType type,

        @Size(max = 50)
        String icon,

        Long parentId
) {
}
