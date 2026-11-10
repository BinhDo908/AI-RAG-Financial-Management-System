package com.fintech.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity thuộc về một người dùng. Chủ sở hữu được lưu bằng cột {@code user_id} (không map quan hệ)
 * để mọi truy vấn trong DAO lọc theo userId một cách tường minh (BR-05, NFR-04).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class UserOwnedEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
}
