package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.CategoryDao;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.enums.CategoryType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class CategoryDaoImpl extends AbstractHibernateDao<Category> implements CategoryDao {

    public CategoryDaoImpl() {
        super(Category.class);
    }

    @Override
    public List<Category> findDefaults(CategoryType type, boolean includeHidden) {
        return query("""
                from Category c
                where c.userId is null
                  and (:type is null or c.type = :type)
                  and (:includeHidden = true or c.hidden = false)
                order by c.type, c.id
                """)
                .setParameter("type", type)
                .setParameter("includeHidden", includeHidden)
                .getResultList();
    }

    @Override
    public Optional<Category> findDefaultById(Long id) {
        return query("from Category c where c.id = :id and c.userId is null")
                .setParameter("id", id)
                .uniqueResultOptional();
    }

    @Override
    public Optional<Category> findAccessible(Long id, Long userId) {
        return query("from Category c where c.id = :id and (c.userId is null or c.userId = :userId)")
                .setParameter("id", id)
                .setParameter("userId", userId)
                .uniqueResultOptional();
    }

    @Override
    public List<Long> findChildIds(Long parentId) {
        return session().createSelectionQuery("select c.id from Category c where c.parentId = :parentId", Long.class)
                .setParameter("parentId", parentId)
                .getResultList();
    }

    @Override
    public List<Category> findVisibleForUser(Long userId, CategoryType type) {
        return query("""
                from Category c
                where (c.userId is null or c.userId = :userId)
                  and c.hidden = false
                  and (:type is null or c.type = :type)
                order by c.type, case when c.userId is null then 0 else 1 end, c.id
                """)
                .setParameter("userId", userId)
                .setParameter("type", type)
                .getResultList();
    }

    @Override
    public List<Category> findOwnedByUser(Long userId) {
        return query("from Category c where c.userId = :userId order by c.id")
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public boolean existsByName(Long userId, CategoryType type, String name, Long excludeId) {
        String scope = userId == null ? "c.userId is null" : "c.userId = :userId";
        var q = session().createSelectionQuery("""
                select count(c) from Category c
                where %s
                  and c.type = :type
                  and lower(c.name) = :name
                  and (:excludeId is null or c.id <> :excludeId)
                """.formatted(scope), Long.class)
                .setParameter("type", type)
                .setParameter("name", name.trim().toLowerCase(Locale.ROOT))
                .setParameter("excludeId", excludeId);
        if (userId != null) {
            q.setParameter("userId", userId);
        }
        return q.getSingleResult() > 0;
    }

    @Override
    public boolean isInUse(Long categoryId) {
        String[] checks = {
                "select count(t) from Transaction t where t.categoryId = :id",
                "select count(b) from Budget b where b.categoryId = :id",
                "select count(r) from RecurringRule r where r.categoryId = :id",
                "select count(c) from Category c where c.parentId = :id"
        };
        for (String hql : checks) {
            long n = session().createSelectionQuery(hql, Long.class)
                    .setParameter("id", categoryId)
                    .getSingleResult();
            if (n > 0) {
                return true;
            }
        }
        return false;
    }
}
