-- =====================================================================
-- V5 (H2, chỉ dùng khi test): H2 không có pgvector nên lưu vector dạng văn bản "[0.1,0.2,…]"
-- (cùng định dạng pgvector nhận) và tính cosine trong Java.
-- =====================================================================
CREATE TABLE chunk_embeddings (
    chunk_id    BIGINT       PRIMARY KEY REFERENCES chunks (id) ON DELETE CASCADE,
    model       VARCHAR(100) NOT NULL,
    embedding   VARCHAR      NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
