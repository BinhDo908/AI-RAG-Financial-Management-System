package com.fintech.chatbot.service.support;

import com.fintech.chatbot.entity.enums.RecurrenceFrequency;

import java.time.LocalDate;

/**
 * Tính ngày chạy của giao dịch định kỳ (UC-A07).
 *
 * <p>Lần thứ k luôn tính từ ngày bắt đầu ({@code start + k kỳ}), không cộng dồn từ lần trước,
 * nên quy tắc bắt đầu ngày 31 sẽ chạy ngày cuối tháng ở tháng ngắn rồi quay lại ngày 31 (EX-1).
 */
public final class Recurrence {

    /** Giới hạn vòng lặp an toàn (≈ 100 năm theo ngày). */
    private static final int MAX_STEPS = 40_000;

    private Recurrence() {
    }

    public static LocalDate occurrence(LocalDate start, RecurrenceFrequency frequency, int k) {
        return switch (frequency) {
            case DAILY -> start.plusDays(k);
            case WEEKLY -> start.plusWeeks(k);
            case MONTHLY -> start.plusMonths(k);
            case YEARLY -> start.plusYears(k);
        };
    }

    /** Lần chạy đầu tiên vào hoặc sau ngày {@code date}. */
    public static LocalDate firstOnOrAfter(LocalDate start, RecurrenceFrequency frequency, LocalDate date) {
        for (int k = 0; k < MAX_STEPS; k++) {
            LocalDate d = occurrence(start, frequency, k);
            if (!d.isBefore(date)) {
                return d;
            }
        }
        throw new IllegalStateException("Không tìm được ngày chạy kế tiếp");
    }

    /** Lần chạy đầu tiên sau ngày {@code date}. */
    public static LocalDate firstAfter(LocalDate start, RecurrenceFrequency frequency, LocalDate date) {
        return firstOnOrAfter(start, frequency, date.plusDays(1));
    }
}
