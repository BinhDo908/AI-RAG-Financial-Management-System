package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Document;
import com.fintech.chatbot.entity.enums.DocumentStatus;

import java.util.List;
import java.util.Optional;

/**
 * DAO cho E-12 Document (UC-C01).
 */
public interface DocumentDao extends GenericDao<Document, Long> {

    List<Document> search(DocumentStatus status, String keyword, int page, int size);

    long countSearch(DocumentStatus status, String keyword);

    Optional<Document> findByContentHash(String contentHash);

    List<Document> findByStatus(DocumentStatus status);
}
