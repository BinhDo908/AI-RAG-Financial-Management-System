package com.fintech.chatbot.controller.admin;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dto.request.ReviewFeedbackRequest;
import com.fintech.chatbot.dto.response.FeedbackResponses;
import com.fintech.chatbot.entity.enums.FeedbackReason;
import com.fintech.chatbot.entity.enums.ReviewStatus;
import com.fintech.chatbot.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Admin - Feedback review", description = "UC-C02 Kiểm duyệt câu trả lời bị báo cáo")
@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "Hàng chờ báo cáo (mặc định OPEN)")
    @GetMapping
    public ApiResponse<PageResponse<FeedbackResponses.ReportSummary>> list(
            @RequestParam(defaultValue = "OPEN") ReviewStatus status,
            @RequestParam(required = false) FeedbackReason reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(feedbackService.reports(status, reason, from, to, page, size));
    }

    @Operation(summary = "Chi tiết: câu hỏi, câu trả lời, các đoạn đã truy xuất kèm điểm")
    @GetMapping("/{id}")
    public ApiResponse<FeedbackResponses.ReportDetail> get(@PathVariable Long id) {
        return ApiResponse.ok(feedbackService.report(id));
    }

    @Operation(summary = "Phân loại nguyên nhân và đóng báo cáo")
    @PutMapping("/{id}")
    public ApiResponse<FeedbackResponses.ReportDetail> review(@PathVariable Long id,
                                                              @Valid @RequestBody ReviewFeedbackRequest request) {
        return ApiResponse.ok(feedbackService.review(id, request), "Đã cập nhật báo cáo");
    }
}
