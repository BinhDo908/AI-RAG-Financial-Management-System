package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.response.ReportOverviewResponse;

import java.time.LocalDate;

/**
 * UC-A09 — Báo cáo & biểu đồ. Mọi con số do code tính (BR-06).
 */
public interface ReportService {

    /**
     * @param from mặc định ngày đầu tháng hiện tại
     * @param to   mặc định hôm nay
     */
    ReportOverviewResponse overview(Long userId, LocalDate from, LocalDate to);
}
