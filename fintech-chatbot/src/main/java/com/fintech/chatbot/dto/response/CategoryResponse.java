package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.CategoryType;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        String icon,
        Long parentId,
        boolean hidden,
        boolean defaultCategory
) {
}
