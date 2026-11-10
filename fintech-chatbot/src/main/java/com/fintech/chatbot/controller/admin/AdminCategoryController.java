package com.fintech.chatbot.controller.admin;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.CategoryRequest;
import com.fintech.chatbot.dto.response.CategoryResponse;
import com.fintech.chatbot.entity.enums.CategoryType;
import com.fintech.chatbot.service.DefaultCategoryService;
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

@Tag(name = "Admin - Default categories", description = "UC-C06 Quản lý danh mục mặc định")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final DefaultCategoryService defaultCategoryService;

    @Operation(summary = "Danh sách danh mục mặc định")
    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(
            @RequestParam(required = false) CategoryType type,
            @RequestParam(defaultValue = "true") boolean includeHidden) {
        return ApiResponse.ok(defaultCategoryService.list(type, includeHidden));
    }

    @Operation(summary = "Thêm danh mục mặc định")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(defaultCategoryService.create(request), "Đã thêm danh mục");
    }

    @Operation(summary = "Sửa danh mục mặc định")
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(defaultCategoryService.update(id, request), "Đã cập nhật danh mục");
    }

    @Operation(summary = "Ẩn danh mục")
    @PostMapping("/{id}/hide")
    public ApiResponse<CategoryResponse> hide(@PathVariable Long id) {
        return ApiResponse.ok(defaultCategoryService.setHidden(id, true));
    }

    @Operation(summary = "Hiện lại danh mục")
    @PostMapping("/{id}/unhide")
    public ApiResponse<CategoryResponse> unhide(@PathVariable Long id) {
        return ApiResponse.ok(defaultCategoryService.setHidden(id, false));
    }

    @Operation(summary = "Xóa danh mục chưa từng được dùng (BR-10)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        defaultCategoryService.delete(id);
        return ApiResponse.message("Đã xóa danh mục");
    }
}
