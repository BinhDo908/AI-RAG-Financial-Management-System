package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.AiConfigHistoryDao;
import com.fintech.chatbot.entity.AiConfigHistory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AiConfigHistoryDaoImpl extends AbstractHibernateDao<AiConfigHistory> implements AiConfigHistoryDao {

    public AiConfigHistoryDaoImpl() {
        super(AiConfigHistory.class);
    }

    @Override
    public List<AiConfigHistory> findRecent(int limit) {
        return query("from AiConfigHistory h order by h.createdAt desc, h.id desc")
                .setMaxResults(limit)
                .getResultList();
    }
}
