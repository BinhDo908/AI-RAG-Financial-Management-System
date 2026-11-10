package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.UserOwnedDao;
import com.fintech.chatbot.entity.UserOwnedEntity;

import java.util.List;
import java.util.Optional;

/**
 * Cài đặt chung cho DAO của entity thuộc về user: mọi truy vấn đều có điều kiện {@code userId}.
 */
public abstract class AbstractUserOwnedDao<T extends UserOwnedEntity>
        extends AbstractHibernateDao<T> implements UserOwnedDao<T, Long> {

    protected AbstractUserOwnedDao(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public Optional<T> findByIdAndUserId(Long id, Long userId) {
        return query("from " + entityName() + " e where e.id = :id and e.userId = :userId")
                .setParameter("id", id)
                .setParameter("userId", userId)
                .uniqueResultOptional();
    }

    @Override
    public List<T> findAllByUserId(Long userId) {
        return query("from " + entityName() + " e where e.userId = :userId order by e.id")
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public long countByUserId(Long userId) {
        return session().createSelectionQuery(
                        "select count(e) from " + entityName() + " e where e.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }
}
