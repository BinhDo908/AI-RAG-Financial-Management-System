package com.fintech.chatbot.service;

import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dto.response.ResetPasswordResponse;
import com.fintech.chatbot.dto.response.UserResponse;
import com.fintech.chatbot.entity.enums.Role;
import com.fintech.chatbot.entity.enums.UserStatus;

/**
 * UC-C05 — Admin quản lý tài khoản. Không có thao tác nào đọc dữ liệu tài chính của user.
 */
public interface UserAdminService {

    PageResponse<UserResponse> search(String keyword, UserStatus status, Role role, int page, int size);

    UserResponse lock(Long adminId, Long userId);

    UserResponse unlock(Long userId);

    /** Đặt mật khẩu tạm thời ngẫu nhiên; mật khẩu chỉ được trả về một lần. */
    ResetPasswordResponse resetPassword(Long userId);

    /** Tạo tài khoản admin nếu email chưa tồn tại (dùng lúc khởi động). @return true nếu vừa tạo. */
    boolean ensureAdmin(String email, String rawPassword, String fullName);
}
