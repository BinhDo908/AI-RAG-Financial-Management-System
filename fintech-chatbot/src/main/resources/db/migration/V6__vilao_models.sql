-- =====================================================================
-- V6: Ngưỡng điểm RAG cho model embedding dg/text-embedding-3-large (Vilao AI).
-- Đo 23/10/2026: đoạn liên quan ≈ 0,57–0,72; không liên quan ≈ 0,23–0,31 → ngưỡng 0,45.
-- Chỉ đổi nếu Admin chưa chỉnh (vẫn là giá trị seed 0.5).
-- =====================================================================
UPDATE ai_config SET config_value = '0.45' WHERE config_key = 'rag.score_threshold' AND config_value = '0.5';
