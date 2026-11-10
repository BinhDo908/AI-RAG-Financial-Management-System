package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.RecurringRuleDao;
import com.fintech.chatbot.entity.RecurringRule;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class RecurringRuleDaoImpl extends AbstractUserOwnedDao<RecurringRule> implements RecurringRuleDao {

    public RecurringRuleDaoImpl() {
        super(RecurringRule.class);
    }

    @Override
    public List<Long> findDueIds(LocalDate today) {
        return session().createSelectionQuery("""
                        select r.id from RecurringRule r
                        where r.active = true and r.nextRunDate <= :today
                        order by r.nextRunDate, r.id
                        """, Long.class)
                .setParameter("today", today)
                .getResultList();
    }

    @Override
    public boolean existsByWallet(Long walletId) {
        return session().createSelectionQuery("select count(r) from RecurringRule r where r.walletId = :w", Long.class)
                .setParameter("w", walletId)
                .getSingleResult() > 0;
    }

    @Override
    public int reassignCategory(Long userId, Long fromCategoryId, Long toCategoryId) {
        return session().createMutationQuery("""
                        update RecurringRule r set r.category.id = :toId
                        where r.userId = :userId and r.category.id = :fromId
                        """)
                .setParameter("toId", toCategoryId)
                .setParameter("userId", userId)
                .setParameter("fromId", fromCategoryId)
                .executeUpdate();
    }
}
