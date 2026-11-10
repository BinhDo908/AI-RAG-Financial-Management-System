package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.FeedbackDao;
import com.fintech.chatbot.entity.Feedback;
import com.fintech.chatbot.entity.enums.FeedbackRating;
import com.fintech.chatbot.entity.enums.FeedbackReason;
import com.fintech.chatbot.entity.enums.ReviewStatus;
import org.hibernate.query.SelectionQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class FeedbackDaoImpl extends AbstractUserOwnedDao<Feedback> implements FeedbackDao {

    private static final String REPORT_WHERE = """
             where f.reviewStatus is not null
               and (:status is null or f.reviewStatus = :status)
               and (:reason is null or f.reason = :reason)
               and (:fromTime is null or f.createdAt >= :fromTime)
               and (:toTime is null or f.createdAt <= :toTime)
            """;

    public FeedbackDaoImpl() {
        super(Feedback.class);
    }

    @Override
    public Optional<Feedback> findByMessageAndUser(Long messageId, Long userId) {
        return query("from Feedback f where f.messageId = :messageId and f.userId = :userId")
                .setParameter("messageId", messageId)
                .setParameter("userId", userId)
                .uniqueResultOptional();
    }

    @Override
    public List<Feedback> searchReports(ReviewStatus status, FeedbackReason reason, LocalDateTime from,
                                        LocalDateTime to, int page, int size) {
        SelectionQuery<Feedback> q = query("from Feedback f" + REPORT_WHERE + " order by f.createdAt desc, f.id desc");
        bind(q, status, reason, from, to);
        return page(q, page, size);
    }

    @Override
    public long countReports(ReviewStatus status, FeedbackReason reason, LocalDateTime from, LocalDateTime to) {
        SelectionQuery<Long> q = session().createSelectionQuery("select count(f) from Feedback f" + REPORT_WHERE, Long.class);
        bind(q, status, reason, from, to);
        return q.getSingleResult();
    }

    @Override
    public Map<FeedbackRating, Long> countByRating(LocalDateTime from, LocalDateTime to) {
        Map<FeedbackRating, Long> result = new EnumMap<>(FeedbackRating.class);
        for (FeedbackRating r : FeedbackRating.values()) {
            result.put(r, 0L);
        }
        session().createSelectionQuery("""
                        select f.rating, count(f) from Feedback f
                        where f.createdAt between :fromTime and :toTime
                        group by f.rating
                        """, Object[].class)
                .setParameter("fromTime", from)
                .setParameter("toTime", to)
                .getResultList()
                .forEach(row -> result.put((FeedbackRating) row[0], (Long) row[1]));
        return result;
    }

    private static void bind(SelectionQuery<?> q, ReviewStatus status, FeedbackReason reason,
                             LocalDateTime from, LocalDateTime to) {
        q.setParameter("status", status)
                .setParameter("reason", reason)
                .setParameter("fromTime", from)
                .setParameter("toTime", to);
    }
}
