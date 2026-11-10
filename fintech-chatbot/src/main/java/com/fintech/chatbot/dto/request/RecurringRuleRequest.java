package com.fintech.chatbot.dto.request;

import com.fintech.chatbot.entity.enums.RecurrenceFrequency;
import com.fintech.chatbot.entity.enums.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UC-A07 — Thêm / sửa quy tắc giao dịch định kỳ (thu hoặc chi).
 */
public record RecurringRuleRequest(
        @NotNull(message = "Vui lòng chọn loại giao dịch") TransactionType type,
        @NotNull(message = "Vui lòng nhập số tiền") @Positive(message = "Số tiền phải lớn hơn 0") @Digits(integer = 15, fraction = 0, message = "Số tiền phải là số nguyên VND") BigDecimal amount,
        @NotNull(message = "Vui lòng chọn ví") Long walletId,
        @NotNull(message = "Vui lòng chọn danh mục") Long categoryId,
        @Size(max = 500) String note,
        @NotNull(message = "Vui lòng chọn tần suất") RecurrenceFrequency frequency,
        @NotNull(message = "Vui lòng chọn ngày bắt đầu") LocalDate startDate,
        boolean autoCreate
) {
}
