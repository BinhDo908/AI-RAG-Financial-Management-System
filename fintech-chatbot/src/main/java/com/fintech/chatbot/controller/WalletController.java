package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.AdjustBalanceRequest;
import com.fintech.chatbot.dto.request.WalletCreateRequest;
import com.fintech.chatbot.dto.request.WalletUpdateRequest;
import com.fintech.chatbot.dto.response.TransactionResult;
import com.fintech.chatbot.dto.response.WalletListResponse;
import com.fintech.chatbot.dto.response.WalletResponse;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.WalletService;
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

@Tag(name = "Wallets", description = "UC-A01 Quản lý ví")
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "Danh sách ví + tổng số dư")
    @GetMapping
    public ApiResponse<WalletListResponse> list(@CurrentUser UserPrincipal me,
                                                @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ApiResponse.ok(walletService.list(me.id(), includeArchived));
    }

    @Operation(summary = "Chi tiết ví")
    @GetMapping("/{id}")
    public ApiResponse<WalletResponse> get(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(walletService.get(me.id(), id));
    }

    @Operation(summary = "Thêm ví")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WalletResponse> create(@CurrentUser UserPrincipal me,
                                              @Valid @RequestBody WalletCreateRequest request) {
        return ApiResponse.ok(walletService.create(me.id(), request), "Đã thêm ví");
    }

    @Operation(summary = "Sửa tên / loại / ví mặc định")
    @PutMapping("/{id}")
    public ApiResponse<WalletResponse> update(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                              @Valid @RequestBody WalletUpdateRequest request) {
        return ApiResponse.ok(walletService.update(me.id(), id, request), "Đã cập nhật ví");
    }

    @Operation(summary = "Điều chỉnh số dư (tạo giao dịch điều chỉnh)")
    @PostMapping("/{id}/adjust-balance")
    public ApiResponse<TransactionResult> adjust(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                                 @Valid @RequestBody AdjustBalanceRequest request) {
        return ApiResponse.ok(walletService.adjustBalance(me.id(), id, request), "Đã điều chỉnh số dư");
    }

    @Operation(summary = "Lưu trữ ví")
    @PostMapping("/{id}/archive")
    public ApiResponse<WalletResponse> archive(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(walletService.setArchived(me.id(), id, true));
    }

    @Operation(summary = "Bỏ lưu trữ ví")
    @PostMapping("/{id}/unarchive")
    public ApiResponse<WalletResponse> unarchive(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(walletService.setArchived(me.id(), id, false));
    }

    @Operation(summary = "Xóa ví chưa có giao dịch")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        walletService.delete(me.id(), id);
        return ApiResponse.message("Đã xóa ví");
    }
}
