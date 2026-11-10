package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.RecurringRuleRequest;
import com.fintech.chatbot.dto.response.RecurringRuleResponse;
import com.fintech.chatbot.dto.response.TransactionResult;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.RecurringRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Recurring rules", description = "UC-A07 Giao dịch định kỳ")
@RestController
@RequestMapping("/api/recurring-rules")
@RequiredArgsConstructor
public class RecurringRuleController {

    private final RecurringRuleService ruleService;

    @Operation(summary = "Danh sách quy tắc định kỳ")
    @GetMapping
    public ApiResponse<List<RecurringRuleResponse>> list(@CurrentUser UserPrincipal me) {
        return ApiResponse.ok(ruleService.list(me.id()));
    }

    @Operation(summary = "Chi tiết quy tắc")
    @GetMapping("/{id}")
    public ApiResponse<RecurringRuleResponse> get(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(ruleService.get(me.id(), id));
    }

    @Operation(summary = "Thêm quy tắc định kỳ")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecurringRuleResponse> create(@CurrentUser UserPrincipal me,
                                                     @Valid @RequestBody RecurringRuleRequest request) {
        return ApiResponse.ok(ruleService.create(me.id(), request), "Đã thêm quy tắc định kỳ");
    }

    @Operation(summary = "AF-2 — Sửa quy tắc (không ảnh hưởng giao dịch đã sinh)")
    @PutMapping("/{id}")
    public ApiResponse<RecurringRuleResponse> update(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                                     @Valid @RequestBody RecurringRuleRequest request) {
        return ApiResponse.ok(ruleService.update(me.id(), id, request), "Đã cập nhật quy tắc");
    }

    @Operation(summary = "AF-1 — Tạm dừng")
    @PostMapping("/{id}/pause")
    public ApiResponse<RecurringRuleResponse> pause(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(ruleService.setActive(me.id(), id, false));
    }

    @Operation(summary = "AF-1 — Bật lại")
    @PostMapping("/{id}/resume")
    public ApiResponse<RecurringRuleResponse> resume(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(ruleService.setActive(me.id(), id, true));
    }

    @Operation(summary = "Xác nhận ghi giao dịch từ thông báo nhắc (mặc định ngày hôm nay)")
    @PostMapping("/{id}/execute")
    public ApiResponse<TransactionResult> execute(
            @CurrentUser UserPrincipal me, @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(ruleService.execute(me.id(), id, date), "Đã ghi giao dịch");
    }

    @Operation(summary = "Xóa quy tắc (giao dịch đã sinh được giữ nguyên)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        ruleService.delete(me.id(), id);
        return ApiResponse.message("Đã xóa quy tắc");
    }
}
