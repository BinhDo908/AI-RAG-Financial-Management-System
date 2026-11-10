package com.fintech.chatbot.service.support;

import com.fintech.chatbot.entity.enums.RecurrenceFrequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceTest {

    private static final LocalDate JAN_31 = LocalDate.of(2026, 1, 31);

    @Test
    void monthlyOnDay31UsesLastDayOfShortMonthsThenReturnsTo31() {
        assertThat(Recurrence.occurrence(JAN_31, RecurrenceFrequency.MONTHLY, 1)).isEqualTo("2026-02-28");
        assertThat(Recurrence.occurrence(JAN_31, RecurrenceFrequency.MONTHLY, 2)).isEqualTo("2026-03-31");
        assertThat(Recurrence.occurrence(JAN_31, RecurrenceFrequency.MONTHLY, 3)).isEqualTo("2026-04-30");
    }

    @Test
    void firstOnOrAfterAndFirstAfter() {
        assertThat(Recurrence.firstOnOrAfter(JAN_31, RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 3, 31)))
                .isEqualTo("2026-03-31");
        assertThat(Recurrence.firstAfter(JAN_31, RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 3, 31)))
                .isEqualTo("2026-04-30");
        assertThat(Recurrence.firstOnOrAfter(JAN_31, RecurrenceFrequency.WEEKLY, LocalDate.of(2026, 2, 1)))
                .isEqualTo("2026-02-07");
        // Ngày bắt đầu trong tương lai thì chính là lần chạy đầu
        assertThat(Recurrence.firstOnOrAfter(JAN_31, RecurrenceFrequency.YEARLY, LocalDate.of(2025, 6, 1)))
                .isEqualTo(JAN_31);
    }
}
