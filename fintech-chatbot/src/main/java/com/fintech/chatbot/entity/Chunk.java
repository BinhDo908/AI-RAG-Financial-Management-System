package com.fintech.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * E-13 — Đoạn văn bản của tài liệu. Vector embedding được lưu ở vector store (pgvector)
 * trong giai đoạn 3, liên kết qua chunk id.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chunks")
public class Chunk extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "document_id", insertable = false, updatable = false)
    private Long documentId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "heading", length = 300)
    private String heading;

    @Column(name = "page_number")
    private Integer pageNumber;

    public void assignDocument(Document value) {
        document = value;
        documentId = value == null ? null : value.getId();
    }
}
