package com.fintech.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E-16 — Một tham số cấu hình AI dạng key-value (UC-C04), ví dụ "rag.top_k" = "8".
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_config")
public class AiConfigEntry extends BaseEntity {

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(name = "updated_by")
    private Long updatedBy;
}
