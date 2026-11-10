package com.fintech.chatbot.common.util;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Tháng ngân sách dạng chuỗi {@code yyyy-MM} (E-05).
 */
public final class Months {

    private Months() {
    }

    public static YearMonth parse(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException(ErrorCode.INVALID_MONTH);
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_MONTH);
        }
    }

    public static String of(LocalDate date) {
        return YearMonth.from(date).toString();
    }
}
