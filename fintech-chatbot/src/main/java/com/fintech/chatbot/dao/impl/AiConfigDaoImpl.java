package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.AiConfigDao;
import com.fintech.chatbot.entity.AiConfigEntry;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AiConfigDaoImpl extends AbstractHibernateDao<AiConfigEntry> implements AiConfigDao {

    public AiConfigDaoImpl() {
        super(AiConfigEntry.class);
    }

    @Override
    public Optional<AiConfigEntry> findByKey(String key) {
        return query("from AiConfigEntry e where e.configKey = :key")
                .setParameter("key", key)
                .uniqueResultOptional();
    }

    @Override
    public List<AiConfigEntry> findAllEntries() {
        return query("from AiConfigEntry e order by e.configKey").getResultList();
    }
}
