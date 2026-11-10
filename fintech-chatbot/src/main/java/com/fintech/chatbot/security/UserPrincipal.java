package com.fintech.chatbot.security;

import com.fintech.chatbot.entity.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Người dùng đã xác thực, được đặt vào SecurityContext bởi JwtAuthFilter.
 */
public record UserPrincipal(Long id, String email, Role role) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
