package com.fintech.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fintech.chatbot.entity.enums.FeedbackRating;
import com.fintech.chatbot.entity.enums.FeedbackReason;
import com.fintech.chatbot.entity.enums.FeedbackRootCause;
import com.fintech.chatbot.entity.enums.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E-14 — Đánh giá / báo cáo câu trả lời AI (UC-B09), được Admin kiểm duyệt (UC-C02).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "feedbacks")
public class Feedback extends UserOwnedEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "message_id", insertable = false, updatable = false)
    private Long messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false, length = 10)
    private FeedbackRating rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 30)
    private FeedbackReason reason;

    @Column(name = "comment", length = 1000)
    private String comment;

    /** null với 👍; OPEN khi 👎 có lý do. */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20)
    private ReviewStatus reviewStatus;

    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    /** UC-C02 bước 3: nguyên nhân do Admin phân loại. */
    @Enumerated(EnumType.STRING)
    @Column(name = "root_cause", length = 30)
    private FeedbackRootCause rootCause;

    public void assignMessage(Message value) {
        message = value;
        messageId = value == null ? null : value.getId();
    }
}
