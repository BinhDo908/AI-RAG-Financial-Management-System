package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.AiConfigEntry;

import java.util.List;
import java.util.Optional;

/**
 * DAO cho E-16 cấu hình AI (UC-C04).
 */
public interface AiConfigDao extends GenericDao<AiConfigEntry, Long> {

    Optional<AiConfigEntry> findByKey(String key);

    List<AiConfigEntry> findAllEntries();
}
