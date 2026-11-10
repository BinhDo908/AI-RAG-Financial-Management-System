package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Message;
import com.fintech.chatbot.entity.enums.InputMode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO cho E-11 Message. Quyền sở hữu được kiểm tra qua Conversation.userId.
 */
public interface MessageDao extends GenericDao<Message, Long> {

    List<Message> findByConversationId(Long conversationId);

    /** {@code limit} tin nhắn gần nhất của hội thoại, theo thứ tự thời gian (làm ngữ cảnh cho LLM). */
    List<Message> findRecentByConversationId(Long conversationId, int limit);

    /** Toàn bộ tin nhắn của một user (dùng cho xuất dữ liệu UC-D03). */
    List<Message> findAllByUserId(Long userId);

    /** Tin nhắn thuộc hội thoại của user (kiểm tra quyền sở hữu, UC-B09). */
    Optional<Message> findByIdAndUserId(Long messageId, Long userId);

    /** Câu hỏi của user ngay trước một câu trả lời (UC-C02). */
    Optional<Message> findPreviousUserMessage(Long conversationId, Long beforeMessageId);

    // ---------- Thống kê UC-C03 (ẩn danh, tổng hợp) ----------

    long countActiveUsers(LocalDateTime from, LocalDateTime to);

    long countConversations(LocalDateTime from, LocalDateTime to);

    long countUserMessages(LocalDateTime from, LocalDateTime to);

    Map<String, Long> countAnswersByIntent(LocalDateTime from, LocalDateTime to);

    Map<InputMode, Long> countUserMessagesByInputMode(LocalDateTime from, LocalDateTime to);

    Double averageLatencyMs(LocalDateTime from, LocalDateTime to);

    long countNoSourceAnswers(LocalDateTime from, LocalDateTime to);

    /** Câu trả lời "không đủ nguồn", mới nhất trước. */
    List<Message> findNoSourceAnswers(LocalDateTime from, LocalDateTime to, int limit);
}
