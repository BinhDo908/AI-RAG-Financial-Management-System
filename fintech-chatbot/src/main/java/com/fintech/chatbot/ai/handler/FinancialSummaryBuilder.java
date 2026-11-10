package com.fintech.chatbot.ai.handler;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintech.chatbot.ai.JsonSupport;
import com.fintech.chatbot.common.util.MoneyFormatter;
import com.fintech.chatbot.dao.AiDataPermissionDao;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dao.WalletDao;
import com.fintech.chatbot.dto.response.BudgetResponse;
import com.fintech.chatbot.entity.AiDataPermission;
import com.fintech.chatbot.entity.Wallet;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.service.BudgetService;
import com.fintech.chatbot.service.DebtService;
import com.fintech.chatbot.service.FinancialProfileService;
import com.fintech.chatbot.service.SavingGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * UC-B02 bước 2 — Tóm tắt tài chính do code tính (BR-06), chỉ gồm nhóm dữ liệu user cho phép (UC-D05, BR-05).
 *
 * <p>Thu chi trung bình tính trên 3 tháng trọn vẹn gần nhất có dữ liệu; nếu chưa có thì dùng tháng hiện tại.
 * Quy tắc 50/30/20: Thiết yếu = các danh mục mặc định thiết yếu bên dưới, Mong muốn = các danh mục chi còn lại,
 * Tiết kiệm = thu − chi.
 */
@Component
@RequiredArgsConstructor
public class FinancialSummaryBuilder {

    static final Set<String> ESSENTIAL_CATEGORIES =
            Set.of("Ăn uống", "Di chuyển", "Nhà ở", "Hóa đơn", "Sức khỏe", "Giáo dục", "Gia đình");
    private static final int WINDOW_MONTHS = 3;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final JsonSupport json;
    private final TransactionDao transactionDao;
    private final WalletDao walletDao;
    private final AiDataPermissionDao permissionDao;
    private final BudgetService budgetService;
    private final DebtService debtService;
    private final SavingGoalService goalService;
    private final FinancialProfileService profileService;
    private final Clock clock;

    public record Summary(ObjectNode data, boolean hasData) {
    }

    public Summary build(Long userId) {
        AiDataPermission p = permissionDao.findByUserId(userId).orElseGet(AiDataPermission::new);
        ObjectNode s = json.object();
        boolean hasData = false;
        LocalDate today = LocalDate.now(clock);

        if (p.isAllowTransactions()) {
            hasData = cashFlow(userId, today, s);
        }
        if (p.isAllowBudgets()) {
            List<BudgetResponse> budgets = budgetService.list(userId, YearMonth.from(today).toString());
            ArrayNode list = s.putArray("budgetsThisMonth");
            budgets.forEach(b -> list.addObject().put("category", b.categoryName()).put("limit", money(b.limitAmount()))
                    .put("spent", money(b.spent())).put("percentUsed", b.percentUsed() + "%").put("status", b.status().name()));
        }
        if (p.isAllowDebts()) {
            var debts = debtService.summary(userId);
            s.put("borrowingRemaining", money(debts.totalBorrowing()));
            s.put("lendingRemaining", money(debts.totalLending()));
        }
        if (p.isAllowGoals()) {
            ArrayNode goals = s.putArray("savingGoals");
            goalService.list(userId).forEach(g -> {
                ObjectNode item = goals.addObject().put("name", g.name()).put("target", money(g.targetAmount()))
                        .put("current", money(g.currentAmount())).put("percent", g.percent() + "%")
                        .put("status", g.status().name());
                if (g.monthlyNeeded() != null) {
                    item.put("neededPerMonth", money(g.monthlyNeeded()));
                }
            });
        }
        if (p.isAllowProfile()) {
            var profile = profileService.get(userId);
            ObjectNode node = s.putObject("profile");
            if (profile.monthlyIncome() != null) {
                node.put("declaredMonthlyIncome", money(profile.monthlyIncome()));
                hasData = true;
            }
            node.put("dependents", profile.dependents());
            node.set("goals", json.toTree(profile.goals()));
            if (profile.riskLevel() != null) {
                node.put("riskLevel", profile.riskLevel().name());
                hasData = true;
            }
        }
        return new Summary(s, hasData);
    }

    /** @return true nếu có ít nhất một tháng có giao dịch thu / chi */
    private boolean cashFlow(Long userId, LocalDate today, ObjectNode s) {
        YearMonth current = YearMonth.from(today);
        var months = transactionDao.monthlyTotals(userId,
                current.minusMonths(WINDOW_MONTHS).atDay(1), current.minusMonths(1).atEndOfMonth());
        LocalDate from;
        LocalDate to;
        int monthCount;
        if (months.isEmpty()) {
            from = current.atDay(1);
            to = today;
            months = transactionDao.monthlyTotals(userId, from, to);
            monthCount = 1;
            s.put("period", "tháng hiện tại (%s – %s)".formatted(from, to));
        } else {
            from = current.minusMonths(WINDOW_MONTHS).atDay(1);
            to = current.minusMonths(1).atEndOfMonth();
            monthCount = months.size();
            s.put("period", "trung bình %d tháng gần nhất có dữ liệu".formatted(monthCount));
        }
        if (months.isEmpty()) {
            return false;
        }
        BigDecimal income = months.stream().map(TransactionDao.MonthAmount::income).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(monthCount), 0, RoundingMode.HALF_UP);
        BigDecimal expense = months.stream().map(TransactionDao.MonthAmount::expense).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(monthCount), 0, RoundingMode.HALF_UP);
        s.put("avgMonthlyIncome", money(income));
        s.put("avgMonthlyExpense", money(expense));
        s.put("avgMonthlySavings", money(income.subtract(expense)));
        if (income.signum() > 0) {
            s.put("savingsRate", percent(income.subtract(expense), income));
        }

        var byCategory = transactionDao.sumByCategory(userId, TransactionType.EXPENSE, from, to);
        BigDecimal totalExpense = byCategory.stream().map(TransactionDao.CategoryAmount::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ArrayNode top = s.putArray("topExpenseCategories");
        byCategory.stream().limit(5).forEach(c -> top.addObject().put("category", c.categoryName())
                .put("avgPerMonth", money(c.amount().divide(BigDecimal.valueOf(monthCount), 0, RoundingMode.HALF_UP)))
                .put("share", percent(c.amount(), totalExpense)));

        if (income.signum() > 0) {
            BigDecimal needs = byCategory.stream().filter(c -> ESSENTIAL_CATEGORIES.contains(c.categoryName()))
                    .map(TransactionDao.CategoryAmount::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(monthCount), 0, RoundingMode.HALF_UP);
            BigDecimal wants = expense.subtract(needs).max(BigDecimal.ZERO);
            ObjectNode rule = s.putObject("rule503020");
            rule.put("needs", percent(needs, income) + " (chuẩn 50%)");
            rule.put("wants", percent(wants, income) + " (chuẩn 30%)");
            rule.put("savings", percent(income.subtract(expense), income) + " (chuẩn 20%)");
        }

        BigDecimal balance = walletDao.findAllByUserId(userId).stream().filter(w -> !w.isArchived())
                .map(Wallet::getCurrentBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        s.put("totalWalletBalance", money(balance));
        if (expense.signum() > 0) {
            s.put("emergencyFundMonths", balance.divide(expense, 1, RoundingMode.HALF_UP).toPlainString() + " tháng chi tiêu");
        }
        return true;
    }

    private static String percent(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return "0%";
        }
        return part.multiply(HUNDRED).divide(total, 1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static String money(BigDecimal amount) {
        return MoneyFormatter.format(amount);
    }
}
