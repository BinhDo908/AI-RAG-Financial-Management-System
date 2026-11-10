package com.fintech.chatbot.scheduler;

import com.fintech.chatbot.service.DebtService;
import com.fintech.chatbot.service.RecurringRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * UC-D04 — Tác vụ nền chạy mỗi ngày lúc 07:00 (giờ Việt Nam):
 * xử lý giao dịch định kỳ đến hạn và nhắc khoản nợ sắp đến hạn.
 * Mỗi quy tắc chạy trong transaction riêng để lỗi của một quy tắc không ảnh hưởng quy tắc khác.
 * Lịch chạy chỉ bật khi {@code app.scheduler.enabled} không bị tắt (xem SchedulingConfig).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReminderJob {

    private final RecurringRuleService recurringRuleService;
    private final DebtService debtService;
    private final Clock clock;

    @Scheduled(cron = "${app.scheduler.daily-cron:0 0 7 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void run() {
        runFor(LocalDate.now(clock));
    }

    /** Tách riêng để test và chạy tay. */
    public void runFor(LocalDate today) {
        List<Long> dueIds = recurringRuleService.findDueRuleIds(today);
        int failed = 0;
        for (Long id : dueIds) {
            try {
                recurringRuleService.processDue(id, today);
            } catch (RuntimeException e) {
                failed++;
                log.error("Xử lý quy tắc định kỳ {} thất bại", id, e);
            }
        }
        int debtReminders = debtService.notifyDueDebts(today);
        log.info("Job nhắc nhở {}: {} quy tắc định kỳ ({} lỗi), {} nhắc nợ", today, dueIds.size(), failed, debtReminders);
    }
}
