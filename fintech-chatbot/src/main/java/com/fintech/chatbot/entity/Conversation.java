package com.fintech.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E-10 — Hội thoại với chatbot (UC-B08).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conversations")
public class Conversation extends UserOwnedEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;
}
