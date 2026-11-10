package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.LoginRequest;
import com.fintech.chatbot.dto.request.RegisterRequest;
import com.fintech.chatbot.dto.response.AuthResponse;

/**
 * UC-D01 Đăng ký, UC-D02 Đăng nhập / Đăng xuất.
 */
public interface AuthService {

    /** Tạo tài khoản USER, ví "Tiền mặt" mặc định và quyền dữ liệu AI mặc định; trả token để vào app ngay. */
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /** Vô hiệu hóa mọi token hiện có của user. */
    void logout(Long userId);
}
