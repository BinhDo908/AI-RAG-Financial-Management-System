package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.ChunkDao;
import com.fintech.chatbot.entity.Chunk;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class ChunkDaoImpl extends AbstractHibernateDao<Chunk> implements ChunkDao {

    public ChunkDaoImpl() {
        super(Chunk.class);
    }

    @Override
    public List<Chunk> findByDocumentId(Long documentId) {
        return query("from Chunk c where c.documentId = :documentId order by c.chunkIndex")
                .setParameter("documentId", documentId)
                .getResultList();
    }

    @Override
    public List<Chunk> findByIdsWithDocument(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return query("select c from Chunk c join fetch c.document where c.id in (:ids)")
                .setParameterList("ids", ids)
                .getResultList();
    }

    @Override
    public long countByDocumentId(Long documentId) {
        return session().createSelectionQuery("select count(c) from Chunk c where c.documentId = :documentId", Long.class)
                .setParameter("documentId", documentId)
                .getSingleResult();
    }

    @Override
    public int deleteByDocumentId(Long documentId) {
        return session().createMutationQuery("delete from Chunk c where c.documentId = :documentId")
                .setParameter("documentId", documentId)
                .executeUpdate();
    }
}
