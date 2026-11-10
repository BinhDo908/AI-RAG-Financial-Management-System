package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.BudgetRequest;
import com.fintech.chatbot.dto.request.BudgetUpdateRequest;
import com.fintech.chatbot.dto.response.BudgetResponse;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.BudgetService;
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

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "Budgets", description = "UC-A04 Quản lý ngân sách")
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final Clock clock;

    @Operation(summary = "Ngân sách theo tháng (mặc định tháng hiện tại)")
    @GetMapping
    public ApiResponse<List<BudgetResponse>> list(@CurrentUser UserPrincipal me,
                                                  @RequestParam(required = false) String month) {
        return ApiResponse.ok(budgetService.list(me.id(), month == null ? YearMonth.now(clock).toString() : month));
    }

    @Operation(summary = "Chi tiết ngân sách (giao dịch: GET /api/transactions?categoryId=&from=&to=)")
    @GetMapping("/{id}")
    public ApiResponse<BudgetResponse> get(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(budgetService.get(me.id(), id));
    }

    @Operation(summary = "Thêm ngân sách")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BudgetResponse> create(@CurrentUser UserPrincipal me,
                                              @Valid @RequestBody BudgetRequest request) {
        return ApiResponse.ok(budgetService.create(me.id(), request), "Đã thêm ngân sách");
    }

    @Operation(summary = "AF-1 — Sao chép ngân sách tháng trước sang tháng chỉ định")
    @PostMapping("/copy-previous")
    public ApiResponse<List<BudgetResponse>> copyPrevious(@CurrentUser UserPrincipal me,
                                                          @RequestParam String month) {
        return ApiResponse.ok(budgetService.copyFromPreviousMonth(me.id(), month));
    }

    @Operation(summary = "Sửa hạn mức / ngưỡng cảnh báo")
    @PutMapping("/{id}")
    public ApiResponse<BudgetResponse> update(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                              @Valid @RequestBody BudgetUpdateRequest request) {
        return ApiResponse.ok(budgetService.update(me.id(), id, request), "Đã cập nhật ngân sách");
    }

    @Operation(summary = "Xóa ngân sách")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        budgetService.delete(me.id(), id);
        return ApiResponse.message("Đã xóa ngân sách");
    }
}
