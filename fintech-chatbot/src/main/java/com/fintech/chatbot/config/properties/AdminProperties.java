package com.fintech.chatbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thông tin tài khoản admin được tạo lúc khởi động (AdminSeeder).
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password, String fullName) {
}
