package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.UserDao;
import com.fintech.chatbot.entity.User;
import org.hibernate.query.SelectionQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class UserDaoImpl extends AbstractHibernateDao<User> implements UserDao {

    private static final String SEARCH_WHERE = """
             where (:keyword is null
                    or lower(u.email) like :keyword
                    or lower(u.fullName) like :keyword)
               and (:status is null or u.status = :status)
               and (:role is null or u.role = :role)
            """;

    public UserDaoImpl() {
        super(User.class);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return query("from User u where lower(u.email) = :email")
                .setParameter("email", normalize(email))
                .uniqueResultOptional();
    }

    @Override
    public boolean existsByEmail(String email) {
        return session().createSelectionQuery(
                        "select count(u) from User u where lower(u.email) = :email", Long.class)
                .setParameter("email", normalize(email))
                .getSingleResult() > 0;
    }

    @Override
    public List<User> search(SearchCriteria criteria, int page, int size) {
        SelectionQuery<User> q = query("from User u" + SEARCH_WHERE + " order by u.createdAt desc");
        bind(q, criteria);
        return page(q, page, size);
    }

    @Override
    public long countSearch(SearchCriteria criteria) {
        SelectionQuery<Long> q = session().createSelectionQuery(
                "select count(u) from User u" + SEARCH_WHERE, Long.class);
        bind(q, criteria);
        return q.getSingleResult();
    }

    @Override
    public void hardDelete(Long userId) {
        session().flush();
        session().clear();
        session().createMutationQuery("delete from User u where u.id = :id")
                .setParameter("id", userId)
                .executeUpdate();
    }

    private static void bind(SelectionQuery<?> q, SearchCriteria c) {
        String keyword = c.keyword() == null || c.keyword().isBlank()
                ? null
                : "%" + c.keyword().trim().toLowerCase(Locale.ROOT) + "%";
        q.setParameter("keyword", keyword)
                .setParameter("status", c.status())
                .setParameter("role", c.role());
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
