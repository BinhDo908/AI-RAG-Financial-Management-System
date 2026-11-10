package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.LoginRequest;
import com.fintech.chatbot.dto.request.RegisterRequest;
import com.fintech.chatbot.dto.response.AuthResponse;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "UC-D01 Đăng ký, UC-D02 Đăng nhập / Đăng xuất")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "UC-D01 — Đăng ký tài khoản")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Đăng ký thành công");
    }

    @Operation(summary = "UC-D02 — Đăng nhập")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "UC-D02 AF-2 — Đăng xuất (vô hiệu hóa mọi token hiện có)")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CurrentUser UserPrincipal me) {
        authService.logout(me.id());
        return ApiResponse.message("Đã đăng xuất");
    }
}
