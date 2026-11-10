package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.response.AiConfigResponse;

import java.util.Map;

/**
 * UC-C04 — Cấu hình AI & guardrail.
 */
public interface AiConfigService {

    /** Giá trị hiện tại, mặc định, model đang hiệu lực, trạng thái kết nối AI và lịch sử thay đổi gần đây. */
    AiConfigResponse get();

    /** Chỉ cập nhật các khóa được gửi lên; mỗi thay đổi được ghi lịch sử. */
    AiConfigResponse update(Long adminId, Map<String, String> values);

    /** AF-1: khôi phục toàn bộ giá trị mặc định. */
    AiConfigResponse resetDefaults(Long adminId);
}
