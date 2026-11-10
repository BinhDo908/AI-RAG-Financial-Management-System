package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.AiConfigHistory;

import java.util.List;

/**
 * DAO cho lịch sử thay đổi cấu hình AI (UC-C04 AF-2).
 */
public interface AiConfigHistoryDao extends GenericDao<AiConfigHistory, Long> {

    List<AiConfigHistory> findRecent(int limit);
}
