package com.fintech.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lịch sử thay đổi cấu hình AI (UC-C04 AF-2).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_config_history")
public class AiConfigHistory extends BaseEntity {

    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value", nullable = false)
    private String newValue;

    @Column(name = "changed_by")
    private Long changedBy;
}
