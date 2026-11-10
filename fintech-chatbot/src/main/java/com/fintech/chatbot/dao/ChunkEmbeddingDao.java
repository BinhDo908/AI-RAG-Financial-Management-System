package com.fintech.chatbot.dao;

import java.util.List;

/**
 * Lưu và tìm kiếm vector của các đoạn tài liệu (bảng {@code chunk_embeddings}, migration V5).
 * PostgreSQL dùng pgvector (khoảng cách cosine {@code <=>}); H2 khi test tính cosine trong Java.
 */
public interface ChunkEmbeddingDao {

    void save(Long chunkId, String model, float[] embedding);

    int deleteByDocumentId(Long documentId);

    long countByDocumentId(Long documentId);

    /**
     * Tìm {@code limit} đoạn gần nhất với vector truy vấn, chỉ trong tài liệu APPROVED
     * và được embed bằng cùng model.
     */
    List<ScoredChunk> searchApproved(float[] query, String model, int limit);

    /** {@code score} = độ tương đồng cosine trong [-1, 1], càng lớn càng liên quan. */
    record ScoredChunk(Long chunkId, double score) {
    }
}
