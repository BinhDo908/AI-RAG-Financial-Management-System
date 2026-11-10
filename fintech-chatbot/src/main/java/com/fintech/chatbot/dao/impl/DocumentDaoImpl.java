package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.DocumentDao;
import com.fintech.chatbot.entity.Document;
import com.fintech.chatbot.entity.enums.DocumentStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class DocumentDaoImpl extends AbstractHibernateDao<Document> implements DocumentDao {

    private static final String WHERE = """
             where (:status is null or d.status = :status)
               and (:keyword is null or lower(d.title) like :keyword)
            """;

    public DocumentDaoImpl() {
        super(Document.class);
    }

    @Override
    public List<Document> search(DocumentStatus status, String keyword, int page, int size) {
        return page(query("from Document d" + WHERE + " order by d.updatedAt desc, d.id desc")
                .setParameter("status", status)
                .setParameter("keyword", like(keyword)), page, size);
    }

    @Override
    public long countSearch(DocumentStatus status, String keyword) {
        return session().createSelectionQuery("select count(d) from Document d" + WHERE, Long.class)
                .setParameter("status", status)
                .setParameter("keyword", like(keyword))
                .getSingleResult();
    }

    @Override
    public Optional<Document> findByContentHash(String contentHash) {
        return query("from Document d where d.contentHash = :hash")
                .setParameter("hash", contentHash)
                .setMaxResults(1)
                .uniqueResultOptional();
    }

    @Override
    public List<Document> findByStatus(DocumentStatus status) {
        return query("from Document d where d.status = :status order by d.id")
                .setParameter("status", status)
                .getResultList();
    }

    private static String like(String keyword) {
        return keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
