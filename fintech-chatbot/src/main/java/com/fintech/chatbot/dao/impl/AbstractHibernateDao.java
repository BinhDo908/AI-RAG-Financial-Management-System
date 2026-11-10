package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.GenericDao;
import com.fintech.chatbot.entity.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Cài đặt chung của DAO bằng Hibernate {@link Session} và HQL.
 *
 * <p>DAO không tự mở transaction: ranh giới transaction nằm ở tầng Service ({@code @Transactional}).
 * {@code Propagation.MANDATORY} bắt buộc mọi lời gọi DAO phải nằm trong một transaction có sẵn,
 * giúp phát hiện sớm lỗi gọi DAO trực tiếp từ controller.
 */
@Transactional(propagation = Propagation.MANDATORY)
public abstract class AbstractHibernateDao<T extends BaseEntity> implements GenericDao<T, Long> {

    protected final Class<T> entityClass;

    @PersistenceContext
    private EntityManager entityManager;

    protected AbstractHibernateDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /** Hibernate Session gắn với transaction hiện tại. */
    protected Session session() {
        return entityManager.unwrap(Session.class);
    }

    protected String entityName() {
        return entityClass.getSimpleName();
    }

    /** Tạo truy vấn HQL trả về entity của DAO này. */
    protected SelectionQuery<T> query(String hql) {
        return session().createSelectionQuery(hql, entityClass);
    }

    protected static <R> List<R> page(SelectionQuery<R> query, int page, int size) {
        return query.setFirstResult(Math.max(page, 0) * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public Optional<T> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(session().get(entityClass, id));
    }

    @Override
    public List<T> findAll(int page, int size) {
        return page(query("from " + entityName() + " e order by e.id"), page, size);
    }

    @Override
    public long count() {
        return session().createSelectionQuery("select count(e) from " + entityName() + " e", Long.class)
                .getSingleResult();
    }

    @Override
    public T save(T entity) {
        if (entity.getId() == null) {
            session().persist(entity);
            return entity;
        }
        return session().merge(entity);
    }

    @Override
    public void delete(T entity) {
        Session s = session();
        s.remove(s.contains(entity) ? entity : s.merge(entity));
    }

    @Override
    public boolean deleteById(Long id) {
        return findById(id).map(e -> {
            session().remove(e);
            return true;
        }).orElse(false);
    }

    @Override
    public void flush() {
        session().flush();
    }
}
