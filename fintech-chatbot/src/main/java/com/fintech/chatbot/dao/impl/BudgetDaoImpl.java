package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.BudgetDao;
import com.fintech.chatbot.entity.Budget;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class BudgetDaoImpl extends AbstractUserOwnedDao<Budget> implements BudgetDao {

    public BudgetDaoImpl() {
        super(Budget.class);
    }

    @Override
    public List<Budget> findByMonth(Long userId, String month) {
        return query("""
                select b from Budget b join fetch b.category
                where b.userId = :userId and b.budgetMonth = :month
                order by b.id
                """)
                .setParameter("userId", userId)
                .setParameter("month", month)
                .getResultList();
    }

    @Override
    public Optional<Budget> findByCategoryAndMonth(Long userId, Long categoryId, String month) {
        return query("from Budget b where b.userId = :userId and b.categoryId = :categoryId and b.budgetMonth = :month")
                .setParameter("userId", userId)
                .setParameter("categoryId", categoryId)
                .setParameter("month", month)
                .uniqueResultOptional();
    }

    @Override
    public List<Budget> findByCategoriesAndMonth(Long userId, Collection<Long> categoryIds, String month) {
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return query("""
                select b from Budget b join fetch b.category
                where b.userId = :userId and b.categoryId in (:categoryIds) and b.budgetMonth = :month
                """)
                .setParameter("userId", userId)
                .setParameterList("categoryIds", categoryIds)
                .setParameter("month", month)
                .getResultList();
    }

    @Override
    public List<Budget> findByCategory(Long userId, Long categoryId) {
        return query("from Budget b where b.userId = :userId and b.categoryId = :categoryId")
                .setParameter("userId", userId)
                .setParameter("categoryId", categoryId)
                .getResultList();
    }
}
