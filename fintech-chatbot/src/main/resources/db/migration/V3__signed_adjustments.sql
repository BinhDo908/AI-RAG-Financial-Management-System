-- =====================================================================
-- V3: Giao dịch ADJUST (điều chỉnh số dư, BR-07) mang dấu: dương = tăng, âm = giảm.
-- Các loại còn lại vẫn bắt buộc số tiền dương.
-- =====================================================================
ALTER TABLE transactions DROP CONSTRAINT ck_transactions_amount_positive;

ALTER TABLE transactions ADD CONSTRAINT ck_transactions_amount_sign
    CHECK ((type = 'ADJUST' AND amount <> 0) OR (type <> 'ADJUST' AND amount > 0));
