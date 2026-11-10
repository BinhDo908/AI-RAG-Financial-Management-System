package com.fintech.chatbot.controller.admin;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dto.response.ResetPasswordResponse;
import com.fintech.chatbot.dto.response.UserResponse;
import com.fintech.chatbot.entity.enums.Role;
import com.fintech.chatbot.entity.enums.UserStatus;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Users", description = "UC-C05 Quản lý tài khoản người dùng")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAdminService userAdminService;

    @Operation(summary = "Danh sách / tìm kiếm tài khoản")
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userAdminService.search(keyword, status, role, page, size));
    }

    @Operation(summary = "Khóa tài khoản (đăng xuất user khỏi mọi thiết bị)")
    @PostMapping("/{id}/lock")
    public ApiResponse<UserResponse> lock(@CurrentUser UserPrincipal admin, @PathVariable Long id) {
        return ApiResponse.ok(userAdminService.lock(admin.id(), id), "Đã khóa tài khoản");
    }

    @Operation(summary = "Mở khóa tài khoản")
    @PostMapping("/{id}/unlock")
    public ApiResponse<UserResponse> unlock(@PathVariable Long id) {
        return ApiResponse.ok(userAdminService.unlock(id), "Đã mở khóa tài khoản");
    }

    @Operation(summary = "Đặt lại mật khẩu tạm thời",
            description = "Mật khẩu tạm chỉ hiển thị một lần; gửi cho user qua kênh an toàn và yêu cầu đổi ngay.")
    @PostMapping("/{id}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        return ApiResponse.ok(userAdminService.resetPassword(id));
    }
}
