package com.fintech.chatbot.service.support;

import com.fintech.chatbot.common.util.MoneyFormatter;
import com.fintech.chatbot.common.util.Months;
import com.fintech.chatbot.dao.BudgetDao;
import com.fintech.chatbot.dao.CategoryDao;
import com.fintech.chatbot.dto.response.BudgetResponse;
import com.fintech.chatbot.entity.Budget;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * UC-B06 — Cảnh báo ngân sách dựa trên số đã chi thực tế.
 *
 * <p>Gọi sau mỗi lần giao dịch chi được thêm / sửa. Mỗi mức (cảnh báo, vượt) chỉ gửi
 * một lần cho mỗi ngân sách; vì ngân sách theo tháng nên tương đương một lần / danh mục / tháng (EX-1).
 * Ngân sách của danh mục cha cũng được kiểm tra vì nó tính cả chi của danh mục con.
 */
@Component
@RequiredArgsConstructor
public class BudgetAlertService {

    private final BudgetDao budgetDao;
    private final CategoryDao categoryDao;
    private final BudgetCalculator budgetCalculator;
    private final NotificationPublisher notificationPublisher;
    private final Clock clock;

    public void checkAfterExpense(Long userId, Long categoryId, LocalDate txnDate) {
        if (categoryId == null) {
            return;
        }
        List<Long> categoryIds = new ArrayList<>();
        categoryIds.add(categoryId);
        categoryDao.findById(categoryId).map(Category::getParentId).ifPresent(categoryIds::add);

        for (Budget budget : budgetDao.findByCategoriesAndMonth(userId, categoryIds, Months.of(txnDate))) {
            check(budget);
        }
    }

    public void check(Budget budget) {
        BudgetUsage usage = budgetCalculator.usage(budget);
        BudgetResponse.Status status = usage.status();
        String categoryName = budget.getCategory().getName();
        if (status == BudgetResponse.Status.OVER) {
            notificationPublisher.publishOnce(budget.getUserId(), NotificationType.BUDGET_OVER,
                    "%s đã vượt ngân sách tháng %s: đã chi %s / %s (%d%%)".formatted(
                            categoryName, displayMonth(budget), MoneyFormatter.format(usage.spent()),
                            MoneyFormatter.format(usage.limit()), usage.percentUsed()),
                    budget.getId());
        } else if (status == BudgetResponse.Status.WARNING) {
            notificationPublisher.publishOnce(budget.getUserId(), NotificationType.BUDGET_WARN,
                    "%s đã dùng %d%% ngân sách tháng %s, còn %s%s".formatted(
                            categoryName, usage.percentUsed(), displayMonth(budget),
                            MoneyFormatter.format(usage.remaining()), perDayHint(budget, usage)),
                    budget.getId());
        }
    }

    /** Ví dụ: " cho 9 ngày (~90.000đ/ngày)" — chỉ khi ngân sách là của tháng hiện tại. */
    private String perDayHint(Budget budget, BudgetUsage usage) {
        LocalDate today = LocalDate.now(clock);
        YearMonth month = Months.parse(budget.getBudgetMonth());
        if (!month.equals(YearMonth.from(today))) {
            return "";
        }
        int daysLeft = month.lengthOfMonth() - today.getDayOfMonth() + 1;
        BigDecimal perDay = usage.remaining().divide(BigDecimal.valueOf(daysLeft), 0, RoundingMode.DOWN);
        return " cho %d ngày (~%s/ngày)".formatted(daysLeft, MoneyFormatter.format(perDay));
    }

    private static String displayMonth(Budget budget) {
        YearMonth month = Months.parse(budget.getBudgetMonth());
        return "%02d/%d".formatted(month.getMonthValue(), month.getYear());
    }
}
