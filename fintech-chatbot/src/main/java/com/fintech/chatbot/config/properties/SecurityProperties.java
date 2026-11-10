package com.fintech.chatbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tham số khóa đăng nhập tạm thời (UC-D02 EX-2).
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(int maxFailedLogins, int loginLockMinutes) {
}
