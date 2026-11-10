package com.fintech.chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Đồng hồ hệ thống theo giờ Việt Nam. Inject {@link Clock} thay vì gọi now() trực tiếp để test được.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
