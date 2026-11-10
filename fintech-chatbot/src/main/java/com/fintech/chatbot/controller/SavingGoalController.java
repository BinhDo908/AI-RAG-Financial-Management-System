package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.SavingGoalRequest;
import com.fintech.chatbot.dto.request.WalletAmountRequest;
import com.fintech.chatbot.dto.response.SavingGoalResponse;
import com.fintech.chatbot.dto.response.TransactionResponse;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.SavingGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Saving goals", description = "UC-A05 Mục tiêu tiết kiệm")
@RestController
@RequestMapping("/api/saving-goals")
@RequiredArgsConstructor
public class SavingGoalController {

    private final SavingGoalService goalService;

    @Operation(summary = "Danh sách mục tiêu kèm tiến độ")
    @GetMapping
    public ApiResponse<List<SavingGoalResponse>> list(@CurrentUser UserPrincipal me) {
        return ApiResponse.ok(goalService.list(me.id()));
    }

    @Operation(summary = "Chi tiết mục tiêu")
    @GetMapping("/{id}")
    public ApiResponse<SavingGoalResponse> get(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(goalService.get(me.id(), id));
    }

    @Operation(summary = "Lịch sử nạp / rút của mục tiêu")
    @GetMapping("/{id}/transactions")
    public ApiResponse<List<TransactionResponse>> transactions(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(goalService.transactions(me.id(), id));
    }

    @Operation(summary = "Thêm mục tiêu")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SavingGoalResponse> create(@CurrentUser UserPrincipal me,
                                                  @Valid @RequestBody SavingGoalRequest request) {
        return ApiResponse.ok(goalService.create(me.id(), request), "Đã thêm mục tiêu");
    }

    @Operation(summary = "Sửa mục tiêu")
    @PutMapping("/{id}")
    public ApiResponse<SavingGoalResponse> update(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                                  @Valid @RequestBody SavingGoalRequest request) {
        return ApiResponse.ok(goalService.update(me.id(), id, request), "Đã cập nhật mục tiêu");
    }

    @Operation(summary = "Nạp tiền từ ví vào mục tiêu")
    @PostMapping("/{id}/deposit")
    public ApiResponse<SavingGoalResponse> deposit(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                                   @Valid @RequestBody WalletAmountRequest request) {
        return ApiResponse.ok(goalService.deposit(me.id(), id, request), "Đã nạp tiền vào mục tiêu");
    }

    @Operation(summary = "AF-1 — Rút tiền từ mục tiêu về ví")
    @PostMapping("/{id}/withdraw")
    public ApiResponse<SavingGoalResponse> withdraw(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                                    @Valid @RequestBody WalletAmountRequest request) {
        return ApiResponse.ok(goalService.withdraw(me.id(), id, request), "Đã rút tiền về ví");
    }

    @Operation(summary = "AF-2 — Hủy mục tiêu")
    @PostMapping("/{id}/cancel")
    public ApiResponse<SavingGoalResponse> cancel(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(goalService.cancel(me.id(), id), "Đã hủy mục tiêu");
    }
}
