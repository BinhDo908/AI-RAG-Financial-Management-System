package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Chunk;

import java.util.Collection;
import java.util.List;

/**
 * DAO cho E-13 Chunk (UC-C01).
 */
public interface ChunkDao extends GenericDao<Chunk, Long> {

    List<Chunk> findByDocumentId(Long documentId);

    /** Nạp kèm tài liệu (để dựng trích dẫn); thứ tự kết quả không theo thứ tự id đầu vào. */
    List<Chunk> findByIdsWithDocument(Collection<Long> ids);

    long countByDocumentId(Long documentId);

    int deleteByDocumentId(Long documentId);
}
