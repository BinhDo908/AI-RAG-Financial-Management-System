package com.fintech.chatbot.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * UC-D01 — Đăng ký.
 */
public record RegisterRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$", message = "Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số")
        String password,

        @NotBlank(message = "Vui lòng xác nhận mật khẩu")
        String confirmPassword,

        @AssertTrue(message = "Bạn cần đồng ý điều khoản sử dụng và lưu ý về tư vấn tài chính")
        boolean acceptTerms
) {
}
