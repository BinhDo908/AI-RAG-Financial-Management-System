package com.fintech.chatbot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * UC-A09 — Dashboard tổng quan tài chính cá nhân.
 */
public record ReportOverviewResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal net,
        List<CategoryShare> expenseByCategory,
        List<CategoryShare> topExpenseCategories,
        List<MonthlyTotal> monthlyTrend,
        List<BalancePoint> balanceHistory,
        NetWorth netWorth
) {

    /** Phần chi của một danh mục; {@code percent} trên tổng chi của kỳ. */
    public record CategoryShare(Long categoryId, String categoryName, BigDecimal amount, double percent) {
    }

    /** {@code month} dạng yyyy-MM. */
    public record MonthlyTotal(String month, BigDecimal income, BigDecimal expense) {
    }

    /** Tổng số dư mọi ví tại cuối ngày {@code date}. */
    public record BalancePoint(LocalDate date, BigDecimal totalBalance) {
    }

    /** Tài sản ròng = tổng số dư ví + khoản cho vay còn lại − khoản đi vay còn lại. */
    public record NetWorth(BigDecimal walletBalance, BigDecimal lending, BigDecimal borrowing, BigDecimal netWorth) {
    }
}
