package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.AiDataPermissionRequest;
import com.fintech.chatbot.dto.request.ChangePasswordRequest;
import com.fintech.chatbot.dto.request.DeleteAccountRequest;
import com.fintech.chatbot.dto.request.UpdateProfileRequest;
import com.fintech.chatbot.dto.response.AiDataPermissionResponse;
import com.fintech.chatbot.dto.response.UserResponse;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.AccountService;
import com.fintech.chatbot.service.AiDataPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Account", description = "UC-D03 Quản lý hồ sơ cá nhân, UC-D05 Quyền dữ liệu AI")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AiDataPermissionService aiDataPermissionService;

    @Operation(summary = "Xem thông tin tài khoản đang đăng nhập")
    @GetMapping
    public ApiResponse<UserResponse> me(@CurrentUser UserPrincipal me) {
        return ApiResponse.ok(accountService.getProfile(me.id()));
    }

    @Operation(summary = "Sửa họ tên")
    @PutMapping
    public ApiResponse<UserResponse> update(@CurrentUser UserPrincipal me,
                                            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(accountService.updateProfile(me.id(), request), "Đã cập nhật");
    }

    @Operation(summary = "Đổi mật khẩu (đăng xuất khỏi mọi thiết bị)")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@CurrentUser UserPrincipal me,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(me.id(), request);
        return ApiResponse.message("Đã đổi mật khẩu, vui lòng đăng nhập lại");
    }

    @Operation(summary = "UC-D05 — Xem quyền dữ liệu cho AI")
    @GetMapping("/ai-permissions")
    public ApiResponse<AiDataPermissionResponse> aiPermissions(@CurrentUser UserPrincipal me) {
        return ApiResponse.ok(aiDataPermissionService.get(me.id()));
    }

    @Operation(summary = "UC-D05 — Bật / tắt nhóm dữ liệu AI được dùng")
    @PutMapping("/ai-permissions")
    public ApiResponse<AiDataPermissionResponse> updateAiPermissions(@CurrentUser UserPrincipal me,
                                                                     @RequestBody AiDataPermissionRequest request) {
        return ApiResponse.ok(aiDataPermissionService.update(me.id(), request), "Đã lưu quyền dữ liệu AI");
    }

    @Operation(summary = "AF-1 — Xuất toàn bộ dữ liệu (ZIP chứa JSON)")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@CurrentUser UserPrincipal me) {
        byte[] zip = accountService.exportData(me.id());
        String filename = "fintech-data-" + LocalDate.now() + ".zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(zip);
    }

    @Operation(summary = "AF-2 — Xóa vĩnh viễn tài khoản và dữ liệu",
            description = "Trường confirmation phải đúng: " + DeleteAccountRequest.CONFIRMATION_TEXT)
    @DeleteMapping
    public ApiResponse<Void> delete(@CurrentUser UserPrincipal me,
                                    @Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteAccount(me.id(), request);
        return ApiResponse.message("Đã xóa tài khoản");
    }
}
