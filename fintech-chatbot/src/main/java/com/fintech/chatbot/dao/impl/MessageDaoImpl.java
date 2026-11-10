package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.MessageDao;
import com.fintech.chatbot.entity.Message;
import com.fintech.chatbot.entity.enums.InputMode;
import com.fintech.chatbot.entity.enums.MessageRole;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MessageDaoImpl extends AbstractHibernateDao<Message> implements MessageDao {

    public MessageDaoImpl() {
        super(Message.class);
    }

    @Override
    public List<Message> findByConversationId(Long conversationId) {
        return query("from Message m where m.conversationId = :cid order by m.createdAt, m.id")
                .setParameter("cid", conversationId)
                .getResultList();
    }

    @Override
    public List<Message> findRecentByConversationId(Long conversationId, int limit) {
        List<Message> recent = new ArrayList<>(query(
                        "from Message m where m.conversationId = :cid order by m.createdAt desc, m.id desc")
                .setParameter("cid", conversationId)
                .setMaxResults(limit)
                .getResultList());
        Collections.reverse(recent);
        return recent;
    }

    @Override
    public List<Message> findAllByUserId(Long userId) {
        return query("""
                from Message m
                where m.conversationId in (select c.id from Conversation c where c.userId = :userId)
                order by m.conversationId, m.createdAt, m.id
                """)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public Optional<Message> findByIdAndUserId(Long messageId, Long userId) {
        return query("""
                from Message m
                where m.id = :id
                  and m.conversationId in (select c.id from Conversation c where c.userId = :userId)
                """)
                .setParameter("id", messageId)
                .setParameter("userId", userId)
                .uniqueResultOptional();
    }

    @Override
    public Optional<Message> findPreviousUserMessage(Long conversationId, Long beforeMessageId) {
        return query("""
                from Message m
                where m.conversationId = :cid and m.id < :before and m.role = :role
                order by m.id desc
                """)
                .setParameter("cid", conversationId)
                .setParameter("before", beforeMessageId)
                .setParameter("role", MessageRole.USER)
                .setMaxResults(1)
                .uniqueResultOptional();
    }

    @Override
    public long countActiveUsers(LocalDateTime from, LocalDateTime to) {
        return session().createSelectionQuery("""
                        select count(distinct c.userId) from Message m, Conversation c
                        where c.id = m.conversationId and m.createdAt between :fromTime and :toTime
                        """, Long.class)
                .setParameter("fromTime", from)
                .setParameter("toTime", to)
                .getSingleResult();
    }

    @Override
    public long countConversations(LocalDateTime from, LocalDateTime to) {
        return session().createSelectionQuery("""
                        select count(distinct m.conversationId) from Message m
                        where m.createdAt between :fromTime and :toTime
                        """, Long.class)
                .setParameter("fromTime", from)
                .setParameter("toTime", to)
                .getSingleResult();
    }

    @Override
    public long countUserMessages(LocalDateTime from, LocalDateTime to) {
        return session().createSelectionQuery("""
                        select count(m) from Message m
                        where m.role = :role and m.createdAt between :fromTime and :toTime
                        """, Long.class)
                .setParameter("role", MessageRole.USER)
                .setParameter("fromTime", from)
                .setParameter("toTime", to)
                .getSingleResult();
    }

    @Override
    public Map<String, Long> countAnswersByIntent(LocalDateTime from, LocalDateTime to) {
        Map<String, Long> result = new LinkedHashMap<>();
        session().createSelectionQuery("""
                        select m.intent, count(m) from Message m
                        where m.role = :role and m.intent is not null and m.createdAt between :fromTime and :toTime
                        group by m.intent order by count(m) desc
                        """, Object[].class)
                .setParameter("role", MessageRole.ASSISTANT)
                .setParameter("fromTime", from)
                .setParameter("toTime", to)
                .getResultList()
                .forEach(r -> result.put((String) r[0], (Long) r[1]));
        return result;
    }

    @Override
    public Map<InputMode, Long> countUserMessagesByInputMode(LocalDateTime from, LocalDateTime to) {
        Map<InputMode, Long> result = new EnumMap<>(InputMode.class);
        for (InputMode mode : InputMode.values()) {
            result.put(mode, 0L);
        }
        session().createSelectionQuery("""
                        select m.inputMode, count(m) from Message m
                        where m.role = :role and m.createdAt between :fromTime and :toTime
                        group by m.inputMode
                        """, Object[].class)
                .setParameter("role", MessageRole.USER)
                .setParameter("fromTime", from)
                .setParameter("toTime", to)
                .getResultList()
                .forEach(r -> result.put((InputMode) r[0], (Long) r[1]));
        return result;
    }

    @Override
    public Double averageLatencyMs(LocalDateTime from, LocalDateTime to) {
        return session().createSelectionQuery("""
                        select avg(m.latencyMs) from Message m
                        where m.role = :role and m.latencyMs is not null and m.createdAt between :fromTime and :toTime
                        """, Double.class)
                .setParameter("role", MessageRole.ASSISTANT)
                .setParameter("fromTime", from)
                .setParameter("toTime", to)
                .getSingleResult();
    }

    @Override
    public long countNoSourceAnswers(LocalDateTime from, LocalDateTime to) {
        return session().createSelectionQuery("""
                        select count(m) from Message m
                        where m.noSource = true and m.createdAt between :fromTime and :toTime
                        """, Long.class)
                .setParameter("fromTime", from)
                .setParameter("toTime", to)
                .getSingleResult();
    }

    @Override
    public List<Message> findNoSourceAnswers(LocalDateTime from, LocalDateTime to, int limit) {
        return query("""
                from Message m
                where m.noSource = true and m.createdAt between :fromTime and :toTime
                order by m.createdAt desc
                """)
                .setParameter("fromTime", from)
                .setParameter("toTime", to)
                .setMaxResults(limit)
                .getResultList();
    }
}
