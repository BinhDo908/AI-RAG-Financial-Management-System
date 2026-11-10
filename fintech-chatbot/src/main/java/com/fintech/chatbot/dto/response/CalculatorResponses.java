package com.fintech.chatbot.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Kết quả các công cụ mô phỏng UC-B05. Số tiền đã làm tròn đến đồng.
 */
public final class CalculatorResponses {

    private CalculatorResponses() {
    }

    /** (1) Lãi kép có góp định kỳ; {@code schedule} tóm tắt theo từng năm. */
    public record CompoundInterest(
            BigDecimal futureValue,
            BigDecimal totalContributed,
            BigDecimal totalInterest,
            List<YearRow> schedule
    ) {
    }

    public record YearRow(int year, BigDecimal contributed, BigDecimal interest, BigDecimal balance) {
    }

    /** (2)(3)(4) Lịch trả nợ theo tháng. */
    public record Loan(
            Method method,
            BigDecimal firstPayment,
            BigDecimal lastPayment,
            BigDecimal totalPayment,
            BigDecimal totalInterest,
            List<LoanRow> schedule
    ) {
    }

    public record LoanRow(int period, BigDecimal payment, BigDecimal principal, BigDecimal interest,
                          BigDecimal remaining) {
    }

    public enum Method {
        /** Dư nợ giảm dần, trả đều mỗi kỳ (annuity). */
        ANNUITY,
        /** Dư nợ giảm dần, gốc trả đều. */
        EQUAL_PRINCIPAL,
        /** Lãi phẳng: lãi tính trên gốc ban đầu. */
        FLAT
    }

    /** AF-1: so sánh lãi phẳng với dư nợ giảm dần trả đều cùng lãi suất danh nghĩa. */
    public record LoanComparison(Loan flat, Loan annuity, BigDecimal extraInterestOfFlat) {
    }

    /** (5) Số tiền cần góp mỗi tháng để đạt mục tiêu. */
    public record SavingTarget(
            BigDecimal monthlyContribution,
            BigDecimal totalContributed,
            BigDecimal interestEarned,
            BigDecimal projectedValue
    ) {
    }
}
