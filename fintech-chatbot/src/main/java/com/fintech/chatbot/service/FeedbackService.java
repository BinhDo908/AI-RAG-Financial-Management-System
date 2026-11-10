package com.fintech.chatbot.service;

import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dto.request.FeedbackRequest;
import com.fintech.chatbot.dto.request.ReviewFeedbackRequest;
import com.fintech.chatbot.dto.response.FeedbackResponses;
import com.fintech.chatbot.entity.enums.FeedbackReason;
import com.fintech.chatbot.entity.enums.ReviewStatus;

import java.time.LocalDateTime;

/**
 * UC-B09 Đánh giá câu trả lời (user) và UC-C02 Kiểm duyệt báo cáo (Admin).
 */
public interface FeedbackService {

    /** 👍 lưu đánh giá; 👎 kèm lý do tạo báo cáo OPEN. Đánh giá lại thì ghi đè (AF-2). */
    FeedbackResponses.FeedbackView rate(Long userId, Long messageId, FeedbackRequest request);

    PageResponse<FeedbackResponses.ReportSummary> reports(ReviewStatus status, FeedbackReason reason,
                                                          LocalDateTime from, LocalDateTime to, int page, int size);

    FeedbackResponses.ReportDetail report(Long feedbackId);

    FeedbackResponses.ReportDetail review(Long feedbackId, ReviewFeedbackRequest request);
}
