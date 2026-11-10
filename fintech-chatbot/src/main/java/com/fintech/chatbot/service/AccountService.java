package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.ChangePasswordRequest;
import com.fintech.chatbot.dto.request.DeleteAccountRequest;
import com.fintech.chatbot.dto.request.UpdateProfileRequest;
import com.fintech.chatbot.dto.response.UserResponse;

/**
 * UC-D03 — Quản lý hồ sơ cá nhân.
 */
public interface AccountService {

    UserResponse getProfile(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    /** Đổi mật khẩu và vô hiệu hóa mọi token cũ. */
    void changePassword(Long userId, ChangePasswordRequest request);

    /** AF-1: xuất toàn bộ dữ liệu của user thành file ZIP chứa các file JSON. */
    byte[] exportData(Long userId);

    /** AF-2: xóa vĩnh viễn tài khoản và toàn bộ dữ liệu. */
    void deleteAccount(Long userId, DeleteAccountRequest request);
}
