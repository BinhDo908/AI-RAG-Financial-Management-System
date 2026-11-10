package com.fintech.chatbot.entity;

import com.fintech.chatbot.entity.enums.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * E-12 — Tài liệu trong kho tri thức RAG (UC-C01). Chỉ tài liệu APPROVED được truy xuất (BR-03).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "documents")
public class Document extends BaseEntity {

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    /** Mã nguồn mở trong USE_CASES.md §4.1, ví dụ "SRC-01". */
    @Column(name = "source_id", nullable = false, length = 20)
    private String sourceId;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "license", nullable = false, length = 200)
    private String license;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "accessed_at")
    private LocalDate accessedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.DRAFT;

    /** SHA-256 nội dung, dùng phát hiện tài liệu trùng (UC-C01 EX-3). */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "uploaded_by")
    private Long uploadedBy;
}
