package com.fintech.chatbot.mapper;

import com.fintech.chatbot.dto.response.CategoryResponse;
import com.fintech.chatbot.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Mapping(target = "defaultCategory", expression = "java(category.isDefaultCategory())")
    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponses(List<Category> categories);
}
