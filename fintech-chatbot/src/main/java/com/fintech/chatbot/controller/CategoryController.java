package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.CategoryRequest;
import com.fintech.chatbot.dto.request.MergeCategoryRequest;
import com.fintech.chatbot.dto.response.CategoryResponse;
import com.fintech.chatbot.entity.enums.CategoryType;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.UserCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Categories", description = "UC-A03 Quản lý danh mục thu chi")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final UserCategoryService categoryService;

    @Operation(summary = "Danh mục dùng được: mặc định + danh mục riêng")
    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(@CurrentUser UserPrincipal me,
                                                    @RequestParam(required = false) CategoryType type,
                                                    @RequestParam(defaultValue = "false") boolean includeHidden) {
        return ApiResponse.ok(categoryService.list(me.id(), type, includeHidden));
    }

    @Operation(summary = "Thêm danh mục riêng")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> create(@CurrentUser UserPrincipal me,
                                                @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.create(me.id(), request), "Đã thêm danh mục");
    }

    @Operation(summary = "Sửa danh mục riêng")
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                                @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.update(me.id(), id, request), "Đã cập nhật danh mục");
    }

    @Operation(summary = "AF-2 — Ẩn danh mục riêng")
    @PostMapping("/{id}/hide")
    public ApiResponse<CategoryResponse> hide(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(categoryService.setHidden(me.id(), id, true));
    }

    @Operation(summary = "Hiện lại danh mục riêng")
    @PostMapping("/{id}/unhide")
    public ApiResponse<CategoryResponse> unhide(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(categoryService.setHidden(me.id(), id, false));
    }

    @Operation(summary = "AF-3 — Gộp danh mục riêng vào danh mục khác cùng loại")
    @PostMapping("/{id}/merge")
    public ApiResponse<CategoryResponse> merge(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                               @Valid @RequestBody MergeCategoryRequest request) {
        return ApiResponse.ok(categoryService.merge(me.id(), id, request), "Đã gộp danh mục");
    }

    @Operation(summary = "Xóa danh mục riêng chưa được dùng (BR-10)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        categoryService.delete(me.id(), id);
        return ApiResponse.message("Đã xóa danh mục");
    }
}
