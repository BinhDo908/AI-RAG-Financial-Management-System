package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.ConversationDao;
import com.fintech.chatbot.entity.Conversation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;

@Repository
public class ConversationDaoImpl extends AbstractUserOwnedDao<Conversation> implements ConversationDao {

    private static final String WHERE = """
             where c.userId = :userId and (:keyword is null or lower(c.title) like :keyword)
            """;

    public ConversationDaoImpl() {
        super(Conversation.class);
    }

    @Override
    public List<Conversation> search(Long userId, String keyword, int page, int size) {
        return page(query("from Conversation c" + WHERE + " order by c.updatedAt desc, c.id desc")
                .setParameter("userId", userId)
                .setParameter("keyword", like(keyword)), page, size);
    }

    @Override
    public long countSearch(Long userId, String keyword) {
        return session().createSelectionQuery("select count(c) from Conversation c" + WHERE, Long.class)
                .setParameter("userId", userId)
                .setParameter("keyword", like(keyword))
                .getSingleResult();
    }

    private static String like(String keyword) {
        return keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
