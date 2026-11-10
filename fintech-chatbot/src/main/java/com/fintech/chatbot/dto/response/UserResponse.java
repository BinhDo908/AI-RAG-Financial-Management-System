package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.Role;
import com.fintech.chatbot.entity.enums.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
