package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.DebtCreateRequest;
import com.fintech.chatbot.dto.request.DebtUpdateRequest;
import com.fintech.chatbot.dto.request.WalletAmountRequest;
import com.fintech.chatbot.dto.response.DebtResponse;
import com.fintech.chatbot.dto.response.DebtSummaryResponse;
import com.fintech.chatbot.dto.response.TransactionResponse;
import com.fintech.chatbot.entity.enums.DebtStatus;
import com.fintech.chatbot.entity.enums.DebtType;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.DebtService;
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

@Tag(name = "Debts", description = "UC-A06 Sổ nợ: đi vay / cho vay")
@RestController
@RequestMapping("/api/debts")
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;

    @Operation(summary = "Danh sách khoản nợ")
    @GetMapping
    public ApiResponse<List<DebtResponse>> list(@CurrentUser UserPrincipal me,
                                                @RequestParam(required = false) DebtType type,
                                                @RequestParam(required = false) DebtStatus status) {
        return ApiResponse.ok(debtService.list(me.id(), type, status));
    }

    @Operation(summary = "AF-1 — Tổng đang nợ / cho vay và khoản sắp đến hạn")
    @GetMapping("/summary")
    public ApiResponse<DebtSummaryResponse> summary(@CurrentUser UserPrincipal me) {
        return ApiResponse.ok(debtService.summary(me.id()));
    }

    @Operation(summary = "Chi tiết khoản nợ")
    @GetMapping("/{id}")
    public ApiResponse<DebtResponse> get(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(debtService.get(me.id(), id));
    }

    @Operation(summary = "Lịch sử vay / trả của khoản nợ")
    @GetMapping("/{id}/transactions")
    public ApiResponse<List<TransactionResponse>> transactions(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(debtService.transactions(me.id(), id));
    }

    @Operation(summary = "Ghi khoản đi vay / cho vay")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DebtResponse> create(@CurrentUser UserPrincipal me,
                                            @Valid @RequestBody DebtCreateRequest request) {
        return ApiResponse.ok(debtService.create(me.id(), request), "Đã ghi khoản nợ");
    }

    @Operation(summary = "Sửa người liên quan / hạn trả / ghi chú")
    @PutMapping("/{id}")
    public ApiResponse<DebtResponse> update(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                            @Valid @RequestBody DebtUpdateRequest request) {
        return ApiResponse.ok(debtService.update(me.id(), id, request), "Đã cập nhật khoản nợ");
    }

    @Operation(summary = "Ghi nhận thanh toán (trả nợ hoặc thu nợ)")
    @PostMapping("/{id}/payments")
    public ApiResponse<DebtResponse> pay(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                         @Valid @RequestBody WalletAmountRequest request) {
        return ApiResponse.ok(debtService.recordPayment(me.id(), id, request), "Đã ghi nhận thanh toán");
    }

    @Operation(summary = "Xóa khoản nợ và các giao dịch liên quan")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        debtService.delete(me.id(), id);
        return ApiResponse.message("Đã xóa khoản nợ");
    }
}
