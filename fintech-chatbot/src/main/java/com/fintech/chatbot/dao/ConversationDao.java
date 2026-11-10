package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Conversation;

import java.util.List;

/**
 * DAO cho E-10 Conversation (UC-B08).
 */
public interface ConversationDao extends UserOwnedDao<Conversation, Long> {

    /** Mới cập nhật trước; {@code keyword} tìm trong tiêu đề (null = tất cả). */
    List<Conversation> search(Long userId, String keyword, int page, int size);

    long countSearch(Long userId, String keyword);
}
