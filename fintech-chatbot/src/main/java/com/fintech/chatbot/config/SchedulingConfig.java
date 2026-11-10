package com.fintech.chatbot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật tác vụ nền (UC-D04). Tắt bằng {@code app.scheduler.enabled=false} (ví dụ khi chạy test).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
