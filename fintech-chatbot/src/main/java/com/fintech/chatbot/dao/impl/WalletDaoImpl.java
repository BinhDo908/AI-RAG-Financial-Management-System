package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.WalletDao;
import com.fintech.chatbot.entity.Wallet;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Optional;

@Repository
public class WalletDaoImpl extends AbstractUserOwnedDao<Wallet> implements WalletDao {

    public WalletDaoImpl() {
        super(Wallet.class);
    }

    @Override
    public boolean existsByUserIdAndName(Long userId, String name, Long excludeId) {
        return session().createSelectionQuery("""
                        select count(w) from Wallet w
                        where w.userId = :userId
                          and lower(w.name) = :name
                          and (:excludeId is null or w.id <> :excludeId)
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("name", name.trim().toLowerCase(Locale.ROOT))
                .setParameter("excludeId", excludeId)
                .getSingleResult() > 0;
    }

    @Override
    public Optional<Wallet> findDefaultByUserId(Long userId) {
        return query("from Wallet w where w.userId = :userId and w.defaultWallet = true and w.archived = false")
                .setParameter("userId", userId)
                .setMaxResults(1)
                .uniqueResultOptional();
    }
}
