package com.fintech.chatbot.dto.response;

import com.fintech.chatbot.entity.enums.DocumentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Phản hồi của kho tri thức RAG (UC-C01).
 */
public final class KnowledgeResponses {

    private KnowledgeResponses() {
    }

    /** {@code embeddedCount < chunkCount} với tài liệu APPROVED nghĩa là cần index lại. */
    public record DocumentSummary(Long id, String title, String sourceId, String url, String license,
                                  String language, LocalDate accessedAt, DocumentStatus status,
                                  long chunkCount, long embeddedCount, LocalDateTime updatedAt) {
    }

    public record ChunkPreview(Long id, int index, String heading, Integer pageNumber, int length, String content) {
    }

    /** Bước 6: xem trước các đoạn để Admin rà soát trước khi duyệt. */
    public record DocumentDetail(DocumentSummary document, List<ChunkPreview> chunks) {
    }

    /** AF-3: thử truy vấn — top_k đoạn kèm điểm và đoạn nào được đưa vào prompt. */
    public record RetrievalTest(String question, double threshold, boolean sufficient,
                                List<RetrievalHit> hits) {
    }

    public record RetrievalHit(Long chunkId, Long documentId, String title, String heading, Integer pageNumber,
                               double score, boolean usedInPrompt, String content) {
    }

    /** Kết quả index lại toàn bộ kho. */
    public record ReindexResult(int documents, int chunks) {
    }
}
