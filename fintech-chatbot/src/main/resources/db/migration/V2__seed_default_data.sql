-- =====================================================================
-- V2: Dữ liệu mặc định
--   - Danh mục thu chi mặc định (UC-C06), user_id = NULL
--   - Tham số AI mặc định (UC-C04)
-- Tài khoản admin KHÔNG seed bằng SQL; được AdminSeeder tạo từ biến môi trường.
-- =====================================================================

INSERT INTO categories (user_id, name, type, icon, is_hidden) VALUES
    (NULL, 'Ăn uống',    'EXPENSE', 'food',          FALSE),
    (NULL, 'Di chuyển',  'EXPENSE', 'car',           FALSE),
    (NULL, 'Nhà ở',      'EXPENSE', 'home',          FALSE),
    (NULL, 'Hóa đơn',    'EXPENSE', 'receipt',       FALSE),
    (NULL, 'Mua sắm',    'EXPENSE', 'shopping-bag',  FALSE),
    (NULL, 'Giải trí',   'EXPENSE', 'film',          FALSE),
    (NULL, 'Sức khỏe',   'EXPENSE', 'heart',         FALSE),
    (NULL, 'Giáo dục',   'EXPENSE', 'book',          FALSE),
    (NULL, 'Gia đình',   'EXPENSE', 'users',         FALSE),
    (NULL, 'Khác',       'EXPENSE', 'more',          FALSE),
    (NULL, 'Lương',      'INCOME',  'wallet',        FALSE),
    (NULL, 'Thưởng',     'INCOME',  'gift',          FALSE),
    (NULL, 'Kinh doanh', 'INCOME',  'briefcase',     FALSE),
    (NULL, 'Được tặng',  'INCOME',  'gift',          FALSE),
    (NULL, 'Khác',       'INCOME',  'more',          FALSE);

INSERT INTO ai_config (config_key, config_value) VALUES
    ('llm.model',            'qwen2.5:7b'),
    ('llm.temperature',      '0.2'),
    ('rag.top_k',            '8'),
    ('rag.top_n',            '4'),
    ('rag.score_threshold',  '0.5'),
    ('stt.language',         'vi-VN'),
    ('chat.disclaimer',      'Thông tin chỉ mang tính tham khảo và giáo dục, không phải lời khuyên đầu tư. Hãy cân nhắc tình hình cá nhân trước khi quyết định.'),
    ('chat.blocked_topics',  'khuyến nghị mua bán mã cổ phiếu cụ thể,khuyến nghị mua bán tiền mã hóa cụ thể,vay nặng lãi,cờ bạc');
