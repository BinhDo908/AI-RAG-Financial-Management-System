package com.fintech.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fintech.chatbot.entity.enums.InputMode;
import com.fintech.chatbot.entity.enums.MessageRole;
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
 * E-11 — Tin nhắn trong hội thoại. Không lưu audio, chỉ lưu transcript (BR-09).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "conversation_id", insertable = false, updatable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_mode", nullable = false, length = 10)
    private InputMode inputMode = InputMode.TEXT;

    /** Ý định do Intent Router xác định (USE_CASES.md §9.1). */
    @Column(name = "intent", length = 40)
    private String intent;

    /** Danh sách trích dẫn dạng JSON: [{documentId, chunkId, title, url}]. */
    @Column(name = "citations")
    private String citations;

    /** Dữ liệu có cấu trúc kèm câu trả lời (JSON): thẻ giao dịch nháp, bảng số liệu… */
    @JsonIgnore
    @Column(name = "payload")
    private String payload;

    /** Các đoạn tài liệu đã truy xuất kèm điểm (JSON), để Admin kiểm duyệt (UC-C02). */
    @JsonIgnore
    @Column(name = "retrieval")
    private String retrieval;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    /** true khi chatbot trả lời "không đủ thông tin trong nguồn tham khảo" (UC-B01 AF-1). */
    @Column(name = "no_source", nullable = false)
    private boolean noSource;

    public void assignConversation(Conversation value) {
        conversation = value;
        conversationId = value == null ? null : value.getId();
    }
}
