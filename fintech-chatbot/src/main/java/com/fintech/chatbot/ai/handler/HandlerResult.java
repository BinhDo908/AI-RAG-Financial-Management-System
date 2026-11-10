package com.fintech.chatbot.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.chatbot.ai.Intent;

/**
 * Câu trả lời của một bộ xử lý ý định.
 *
 * @param citations danh sách nguồn trích dẫn (JSON array) hoặc null
 * @param payload   dữ liệu có cấu trúc cho giao diện (thẻ giao dịch nháp, bảng số liệu…) hoặc null
 * @param retrieval đoạn tài liệu đã truy xuất kèm điểm, lưu cho Admin kiểm duyệt (UC-C02) hoặc null
 * @param noSource  true khi trả lời "không đủ thông tin trong nguồn tham khảo"
 */
public record HandlerResult(Intent intent, String content, JsonNode citations, JsonNode payload,
                            JsonNode retrieval, boolean noSource) {

    public static HandlerResult text(Intent intent, String content) {
        return new HandlerResult(intent, content, null, null, null, false);
    }

    public static HandlerResult withPayload(Intent intent, String content, JsonNode payload) {
        return new HandlerResult(intent, content, null, payload, null, false);
    }
}
