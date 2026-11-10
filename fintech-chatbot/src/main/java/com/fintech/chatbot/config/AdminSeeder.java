package com.fintech.chatbot.config;

import com.fintech.chatbot.config.properties.AdminProperties;
import com.fintech.chatbot.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Tạo tài khoản ADMIN lúc khởi động từ biến môi trường ADMIN_EMAIL / ADMIN_PASSWORD.
 * Không có mật khẩu mặc định trong code: nếu chưa đặt ADMIN_PASSWORD thì bỏ qua và ghi cảnh báo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final UserAdminService userAdminService;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminProperties.email()) || !StringUtils.hasText(adminProperties.password())) {
            log.warn("Chưa đặt ADMIN_EMAIL / ADMIN_PASSWORD: bỏ qua việc tạo tài khoản admin");
            return;
        }
        boolean created = userAdminService.ensureAdmin(
                adminProperties.email(), adminProperties.password(), adminProperties.fullName());
        if (created) {
            log.info("Đã tạo tài khoản admin {}", adminProperties.email());
        }
    }
}
