-- =====================================================================
-- V5 (PostgreSQL): vector embedding của các đoạn tài liệu (pgvector).
-- Không cố định số chiều để đổi được model embedding; kho tri thức nhỏ nên tìm kiếm quét toàn bộ (exact).
-- Khi dữ liệu lớn: cố định số chiều rồi thêm index HNSW (vector_cosine_ops).
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE chunk_embeddings (
    chunk_id    BIGINT       PRIMARY KEY REFERENCES chunks (id) ON DELETE CASCADE,
    model       VARCHAR(100) NOT NULL,
    embedding   vector       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
