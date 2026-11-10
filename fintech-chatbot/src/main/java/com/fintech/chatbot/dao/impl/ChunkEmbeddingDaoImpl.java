package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.ChunkEmbeddingDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bảng vector không map thành entity (Hibernate không có sẵn kiểu vector) nên dùng native SQL qua Hibernate Session.
 * Vector được truyền dưới dạng chuỗi "[0.1,0.2,…]" — định dạng pgvector chấp nhận.
 */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class ChunkEmbeddingDaoImpl implements ChunkEmbeddingDao {

    @PersistenceContext
    private EntityManager entityManager;

    private Session session() {
        return entityManager.unwrap(Session.class);
    }

    private boolean isPostgres() {
        return session().getSessionFactory().unwrap(SessionFactoryImplementor.class)
                .getJdbcServices().getDialect() instanceof PostgreSQLDialect;
    }

    @Override
    public void save(Long chunkId, String model, float[] embedding) {
        String vectorValue = isPostgres() ? "cast(:embedding as vector)" : ":embedding";
        session().createNativeMutationQuery("delete from chunk_embeddings where chunk_id = :chunkId")
                .setParameter("chunkId", chunkId)
                .executeUpdate();
        session().createNativeMutationQuery(
                        "insert into chunk_embeddings (chunk_id, model, embedding) values (:chunkId, :model, "
                                + vectorValue + ")")
                .setParameter("chunkId", chunkId)
                .setParameter("model", model)
                .setParameter("embedding", toLiteral(embedding))
                .executeUpdate();
    }

    @Override
    public int deleteByDocumentId(Long documentId) {
        return session().createNativeMutationQuery("""
                        delete from chunk_embeddings
                        where chunk_id in (select id from chunks where document_id = :documentId)
                        """)
                .setParameter("documentId", documentId)
                .executeUpdate();
    }

    @Override
    public long countByDocumentId(Long documentId) {
        Number n = (Number) session().createNativeQuery("""
                        select count(*) from chunk_embeddings e join chunks c on c.id = e.chunk_id
                        where c.document_id = :documentId
                        """, Object.class)
                .setParameter("documentId", documentId)
                .getSingleResult();
        return n.longValue();
    }

    @Override
    public List<ScoredChunk> searchApproved(float[] query, String model, int limit) {
        return isPostgres() ? searchPostgres(query, model, limit) : searchInMemory(query, model, limit);
    }

    /** pgvector: {@code <=>} là khoảng cách cosine (0 = giống hệt) → độ tương đồng = 1 − khoảng cách. */
    private List<ScoredChunk> searchPostgres(float[] query, String model, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = session().createNativeQuery("""
                        select e.chunk_id, 1 - (e.embedding <=> cast(:query as vector)) as score
                        from chunk_embeddings e
                        join chunks c on c.id = e.chunk_id
                        join documents d on d.id = c.document_id
                        where d.status = 'APPROVED' and e.model = :model
                        order by e.embedding <=> cast(:query as vector)
                        limit :limit
                        """, Object[].class)
                .setParameter("query", toLiteral(query))
                .setParameter("model", model)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(r -> new ScoredChunk(((Number) r[0]).longValue(), ((Number) r[1]).doubleValue()))
                .toList();
    }

    /** H2 (test): nạp vector và tính cosine trong Java. */
    private List<ScoredChunk> searchInMemory(float[] query, String model, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = session().createNativeQuery("""
                        select e.chunk_id, e.embedding
                        from chunk_embeddings e
                        join chunks c on c.id = e.chunk_id
                        join documents d on d.id = c.document_id
                        where d.status = 'APPROVED' and e.model = :model
                        """, Object[].class)
                .setParameter("model", model)
                .getResultList();
        List<ScoredChunk> scored = new ArrayList<>();
        for (Object[] r : rows) {
            scored.add(new ScoredChunk(((Number) r[0]).longValue(), cosine(query, parse(r[1].toString()))));
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        return scored.subList(0, Math.min(limit, scored.size()));
    }

    static String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 10).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }

    static float[] parse(String literal) {
        String body = literal.substring(1, literal.length() - 1);
        if (body.isBlank()) {
            return new float[0];
        }
        String[] parts = body.split(",");
        float[] v = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            v[i] = Float.parseFloat(parts[i].trim());
        }
        return v;
    }

    static double cosine(float[] a, float[] b) {
        if (a.length != b.length || a.length == 0) {
            return 0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
