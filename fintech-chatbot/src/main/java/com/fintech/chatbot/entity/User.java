package com.fintech.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fintech.chatbot.entity.enums.Role;
import com.fintech.chatbot.entity.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * E-01 — Tài khoản người dùng.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    /** Tăng khi đăng xuất / đổi mật khẩu để vô hiệu hóa mọi JWT cũ. */
    @JsonIgnore
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @JsonIgnore
    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @JsonIgnore
    @Column(name = "login_locked_until")
    private LocalDateTime loginLockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public void bumpTokenVersion() {
        tokenVersion++;
    }
}
