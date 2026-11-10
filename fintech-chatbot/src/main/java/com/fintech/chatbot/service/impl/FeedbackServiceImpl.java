package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.ai.JsonSupport;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dao.FeedbackDao;
import com.fintech.chatbot.dao.MessageDao;
import com.fintech.chatbot.dto.request.FeedbackRequest;
import com.fintech.chatbot.dto.request.ReviewFeedbackRequest;
import com.fintech.chatbot.dto.response.FeedbackResponses;
import com.fintech.chatbot.entity.Feedback;
import com.fintech.chatbot.entity.Message;
import com.fintech.chatbot.entity.enums.FeedbackRating;
import com.fintech.chatbot.entity.enums.FeedbackReason;
import com.fintech.chatbot.entity.enums.MessageRole;
import com.fintech.chatbot.entity.enums.ReviewStatus;
import com.fintech.chatbot.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private static final int MAX_PAGE_SIZE = 100;

    private final FeedbackDao feedbackDao;
    private final MessageDao messageDao;
    private final JsonSupport json;

    @Override
    @Transactional
    public FeedbackResponses.FeedbackView rate(Long userId, Long messageId, FeedbackRequest request) {
        Message message = messageDao.findByIdAndUserId(messageId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
        if (message.getRole() != MessageRole.ASSISTANT) {
            throw new BusinessException(ErrorCode.FEEDBACK_NOT_ALLOWED);
        }
        Feedback f = feedbackDao.findByMessageAndUser(messageId, userId).orElseGet(() -> {
            Feedback created = new Feedback();
            created.setUserId(userId);
            created.assignMessage(message);
            return created;
        });
        f.setRating(request.rating());
        if (request.rating() == FeedbackRating.DOWN) {
            f.setReason(request.reason() == null ? FeedbackReason.OTHER : request.reason());
            f.setComment(request.comment());
            f.setReviewStatus(ReviewStatus.OPEN);
        } else {
            f.setReason(null);
            f.setComment(null);
            f.setReviewStatus(null);
        }
        f.setRootCause(null);
        f.setAdminNote(null);
        feedbackDao.save(f);
        return new FeedbackResponses.FeedbackView(f.getId(), messageId, f.getRating(), f.getReason(), f.getComment(),
                f.getReviewStatus(), f.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FeedbackResponses.ReportSummary> reports(ReviewStatus status, FeedbackReason reason,
                                                                 LocalDateTime from, LocalDateTime to, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        var items = feedbackDao.searchReports(status, reason, from, to, safePage, safeSize).stream()
                .map(this::summary).toList();
        return PageResponse.of(items, safePage, safeSize, feedbackDao.countReports(status, reason, from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponses.ReportDetail report(Long feedbackId) {
        return detail(loadReport(feedbackId));
    }

    @Override
    @Transactional
    public FeedbackResponses.ReportDetail review(Long feedbackId, ReviewFeedbackRequest request) {
        Feedback f = loadReport(feedbackId);
        if (request.status() == ReviewStatus.OPEN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Trạng thái kiểm duyệt phải là RESOLVED hoặc REJECTED");
        }
        f.setReviewStatus(request.status());
        f.setRootCause(request.rootCause());
        f.setAdminNote(request.adminNote());
        return detail(f);
    }

    private Feedback loadReport(Long feedbackId) {
        return feedbackDao.findById(feedbackId)
                .filter(f -> f.getReviewStatus() != null)
                .orElseThrow(() -> BusinessException.notFound("báo cáo"));
    }

    private FeedbackResponses.ReportSummary summary(Feedback f) {
        return new FeedbackResponses.ReportSummary(f.getId(), f.getMessageId(), f.getMessage().getIntent(),
                f.getReason(), f.getComment(), f.getReviewStatus(), f.getRootCause(), f.getCreatedAt());
    }

    /** Chỉ câu hỏi ngay trước và câu trả lời bị báo cáo — không lộ phần còn lại của hội thoại (UC-B09). */
    private FeedbackResponses.ReportDetail detail(Feedback f) {
        Message answer = f.getMessage();
        String question = messageDao.findPreviousUserMessage(answer.getConversationId(), answer.getId())
                .map(Message::getContent).orElse(null);
        return new FeedbackResponses.ReportDetail(summary(f), question, answer.getContent(),
                json.read(answer.getCitations()), json.read(answer.getRetrieval()), f.getAdminNote());
    }
}
