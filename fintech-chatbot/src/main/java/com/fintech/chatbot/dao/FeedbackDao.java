package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Feedback;
import com.fintech.chatbot.entity.enums.FeedbackRating;
import com.fintech.chatbot.entity.enums.FeedbackReason;
import com.fintech.chatbot.entity.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO cho E-14 Feedback (UC-B09, C02, C03).
 */
public interface FeedbackDao extends UserOwnedDao<Feedback, Long> {

    Optional<Feedback> findByMessageAndUser(Long messageId, Long userId);

    /** UC-C02: hàng chờ báo cáo, lọc theo trạng thái / lý do / thời gian (null = bỏ qua), mới nhất trước. */
    List<Feedback> searchReports(ReviewStatus status, FeedbackReason reason, LocalDateTime from, LocalDateTime to,
                                 int page, int size);

    long countReports(ReviewStatus status, FeedbackReason reason, LocalDateTime from, LocalDateTime to);

    /** UC-C03: số lượt 👍 / 👎 trong khoảng thời gian. */
    Map<FeedbackRating, Long> countByRating(LocalDateTime from, LocalDateTime to);
}
