package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.MarketRateDao;
import com.fintech.chatbot.entity.MarketRate;
import com.fintech.chatbot.entity.enums.MarketRateKind;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MarketRateDaoImpl extends AbstractHibernateDao<MarketRate> implements MarketRateDao {

    public MarketRateDaoImpl() {
        super(MarketRate.class);
    }

    @Override
    public Optional<MarketRate> findLatest(MarketRateKind kind, String symbol) {
        return query("from MarketRate r where r.kind = :kind and r.symbol = :symbol order by r.fetchedAt desc")
                .setParameter("kind", kind)
                .setParameter("symbol", symbol)
                .setMaxResults(1)
                .uniqueResultOptional();
    }
}
