package com.fintech.chatbot.service;

import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dto.response.ConversationDetailResponse;
import com.fintech.chatbot.dto.response.ConversationResponse;

/**
 * UC-B08 — Lịch sử hội thoại. Tin nhắn được tạo bởi chatbot ở giai đoạn 3.
 */
public interface ConversationService {

    /** Mới cập nhật trước; AF-1 tìm theo từ khóa trong tiêu đề. */
    PageResponse<ConversationResponse> list(Long userId, String keyword, int page, int size);

    ConversationDetailResponse get(Long userId, Long conversationId);

    /** "Hội thoại mới"; tiêu đề trống thì dùng tiêu đề mặc định (giai đoạn 3 tự sinh từ câu hỏi đầu). */
    ConversationResponse create(Long userId, String title);

    ConversationResponse rename(Long userId, Long conversationId, String title);

    /** EX-1: xóa hội thoại không xóa giao dịch đã lưu từ hội thoại đó. */
    void delete(Long userId, Long conversationId);
}
