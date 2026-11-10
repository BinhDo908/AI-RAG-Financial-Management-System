package com.fintech.chatbot.entity;

import com.fintech.chatbot.entity.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E-15 — Thông báo trong ứng dụng (UC-B06, UC-D04).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends UserOwnedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    /** Id đối tượng liên quan (ngân sách, khoản nợ, quy tắc định kỳ…) để mở chi tiết. */
    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "is_read", nullable = false)
    private boolean read;
}
