package com.fintech.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Xác thực bằng JWT tự quản lý (AuthService + JwtAuthFilter) nên tắt user mặc định của Spring Security.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class FintechChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(FintechChatbotApplication.class, args);
    }
}
