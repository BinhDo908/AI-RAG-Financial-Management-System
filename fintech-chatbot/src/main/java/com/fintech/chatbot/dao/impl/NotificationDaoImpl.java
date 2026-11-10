package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.NotificationDao;
import com.fintech.chatbot.entity.Notification;
import com.fintech.chatbot.entity.enums.NotificationType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationDaoImpl extends AbstractUserOwnedDao<Notification> implements NotificationDao {

    public NotificationDaoImpl() {
        super(Notification.class);
    }

    @Override
    public List<Notification> findByUser(Long userId, boolean unreadOnly, int page, int size) {
        return page(query("""
                from Notification n
                where n.userId = :userId and (:unreadOnly = false or n.read = false)
                order by n.createdAt desc, n.id desc
                """)
                .setParameter("userId", userId)
                .setParameter("unreadOnly", unreadOnly), page, size);
    }

    @Override
    public long countByUser(Long userId, boolean unreadOnly) {
        return session().createSelectionQuery("""
                        select count(n) from Notification n
                        where n.userId = :userId and (:unreadOnly = false or n.read = false)
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("unreadOnly", unreadOnly)
                .getSingleResult();
    }

    @Override
    public boolean exists(Long userId, NotificationType type, Long refId) {
        return session().createSelectionQuery("""
                        select count(n) from Notification n
                        where n.userId = :userId and n.type = :type and n.refId = :refId
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("type", type)
                .setParameter("refId", refId)
                .getSingleResult() > 0;
    }

    @Override
    public int markAllRead(Long userId) {
        return session().createMutationQuery("update Notification n set n.read = true where n.userId = :userId and n.read = false")
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
