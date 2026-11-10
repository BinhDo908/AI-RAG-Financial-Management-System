package com.fintech.chatbot.service;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.TransactionFilter;
import com.fintech.chatbot.dto.request.AdjustBalanceRequest;
import com.fintech.chatbot.dto.request.BudgetRequest;
import com.fintech.chatbot.dto.request.CategoryRequest;
import com.fintech.chatbot.dto.request.DebtCreateRequest;
import com.fintech.chatbot.dto.request.MergeCategoryRequest;
import com.fintech.chatbot.dto.request.RecurringRuleRequest;
import com.fintech.chatbot.dto.request.RegisterRequest;
import com.fintech.chatbot.dto.request.RiskAnswersRequest;
import com.fintech.chatbot.dto.request.SavingGoalRequest;
import com.fintech.chatbot.dto.request.TransactionRequest;
import com.fintech.chatbot.dto.request.WalletAmountRequest;
import com.fintech.chatbot.dto.request.WalletCreateRequest;
import com.fintech.chatbot.dto.response.BudgetResponse;
import com.fintech.chatbot.dto.response.CategoryResponse;
import com.fintech.chatbot.dto.response.NotificationResponse;
import com.fintech.chatbot.entity.enums.CategoryType;
import com.fintech.chatbot.entity.enums.DebtStatus;
import com.fintech.chatbot.entity.enums.DebtType;
import com.fintech.chatbot.entity.enums.GoalStatus;
import com.fintech.chatbot.entity.enums.NotificationType;
import com.fintech.chatbot.entity.enums.RecurrenceFrequency;
import com.fintech.chatbot.entity.enums.RiskLevel;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.entity.enums.WalletType;
import com.fintech.chatbot.scheduler.DailyReminderJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử nghiệp vụ giai đoạn 2 qua tầng service, DAO Hibernate và H2 thật.
 * Không dùng @Transactional ở test vì job định kỳ chạy transaction REQUIRES_NEW;
 * mỗi test tự tạo user riêng nên dữ liệu không ảnh hưởng nhau.
 * "Hôm nay" cố định là 20/10/2026.
 */
@SpringBootTest
@ActiveProfiles("test")
class FinanceFlowTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
            return Clock.fixed(ZonedDateTime.of(TODAY.atTime(10, 0), zone).toInstant(), zone);
        }
    }

    @Autowired AuthService authService;
    @Autowired WalletService walletService;
    @Autowired TransactionService transactionService;
    @Autowired UserCategoryService categoryService;
    @Autowired BudgetService budgetService;
    @Autowired NotificationService notificationService;
    @Autowired SavingGoalService goalService;
    @Autowired DebtService debtService;
    @Autowired RecurringRuleService ruleService;
    @Autowired ReportService reportService;
    @Autowired ExportService exportService;
    @Autowired FinancialProfileService profileService;
    @Autowired DailyReminderJob reminderJob;

    Long userId;
    Long cashId;
    Long bankId;
    Long foodId;
    Long salaryId;

    @BeforeEach
    void setUp() {
        String email = "flow-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        userId = authService.register(new RegisterRequest("Flow Test", email, "Secret123", "Secret123", true))
                .user().id();
        cashId = walletService.list(userId, false).wallets().get(0).id();
        bankId = walletService.create(userId,
                new WalletCreateRequest("Ngân hàng", WalletType.BANK, money(10_000_000), false)).id();
        foodId = category("Ăn uống", CategoryType.EXPENSE);
        salaryId = category("Lương", CategoryType.INCOME);
    }

    // ---------- UC-A01, A02: số dư ví (BR-07) ----------

    @Test
    void walletBalancesFollowEveryTransactionChange() {
        var income = transactionService.create(userId, txn(TransactionType.INCOME, 15_000_000, bankId, null, salaryId));
        assertThat(income.walletBalance()).isEqualByComparingTo("25000000");

        var expense = transactionService.create(userId, txn(TransactionType.EXPENSE, 50_000, cashId, null, foodId));
        assertThat(expense.walletBalance()).isEqualByComparingTo("-50000");
        assertThat(expense.negativeBalance()).isTrue();

        transactionService.create(userId, txn(TransactionType.TRANSFER, 2_000_000, bankId, cashId, null));
        assertThat(balance(bankId)).isEqualByComparingTo("23000000");
        assertThat(balance(cashId)).isEqualByComparingTo("1950000");

        transactionService.update(userId, expense.transaction().id(),
                txn(TransactionType.EXPENSE, 80_000, cashId, null, foodId));
        assertThat(balance(cashId)).isEqualByComparingTo("1920000");

        // Đổi ví của giao dịch: cả ví cũ và ví mới được tính lại
        transactionService.update(userId, expense.transaction().id(),
                txn(TransactionType.EXPENSE, 80_000, bankId, null, foodId));
        assertThat(balance(cashId)).isEqualByComparingTo("2000000");
        assertThat(balance(bankId)).isEqualByComparingTo("22920000");

        transactionService.delete(userId, income.transaction().id());
        assertThat(balance(bankId)).isEqualByComparingTo("7920000");

        var adjust = walletService.adjustBalance(userId, cashId, new AdjustBalanceRequest(money(1_500_000), null, null));
        assertThat(adjust.transaction().type()).isEqualTo(TransactionType.ADJUST);
        assertThat(adjust.transaction().amount()).isEqualByComparingTo("-500000");
        assertThat(balance(cashId)).isEqualByComparingTo("1500000");

        assertThatThrownBy(() -> walletService.delete(userId, cashId))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_HAS_TRANSACTIONS);
    }

    @Test
    void transactionRulesAreEnforced() {
        assertError(ErrorCode.TRANSFER_SAME_WALLET,
                () -> transactionService.create(userId, txn(TransactionType.TRANSFER, 1_000, cashId, cashId, null)));
        assertError(ErrorCode.CATEGORY_REQUIRED,
                () -> transactionService.create(userId, txn(TransactionType.EXPENSE, 1_000, cashId, null, null)));
        assertError(ErrorCode.CATEGORY_TYPE_MISMATCH,
                () -> transactionService.create(userId, txn(TransactionType.EXPENSE, 1_000, cashId, null, salaryId)));
        assertError(ErrorCode.TRANSACTION_TYPE_NOT_ALLOWED,
                () -> transactionService.create(userId, txn(TransactionType.ADJUST, 1_000, cashId, null, null)));

        walletService.setArchived(userId, bankId, true);
        assertError(ErrorCode.WALLET_ARCHIVED,
                () -> transactionService.create(userId, txn(TransactionType.EXPENSE, 1_000, bankId, null, foodId)));
    }

    @Test
    void usersCannotTouchEachOthersData() {
        String other = "other-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Long otherId = authService.register(new RegisterRequest("Other", other, "Secret123", "Secret123", true))
                .user().id();

        assertError(ErrorCode.NOT_FOUND, () -> walletService.get(otherId, bankId));
        assertError(ErrorCode.NOT_FOUND,
                () -> transactionService.create(otherId, txn(TransactionType.EXPENSE, 1_000, cashId, null, foodId)));
        var t = transactionService.create(userId, txn(TransactionType.EXPENSE, 1_000, cashId, null, foodId));
        assertError(ErrorCode.NOT_FOUND, () -> transactionService.delete(otherId, t.transaction().id()));
    }

    // ---------- UC-A08 ----------

    @Test
    void searchFiltersAndTotals() {
        transactionService.create(userId, txnOn(TransactionType.EXPENSE, 45_000, cashId, foodId, "Cà phê sáng", TODAY));
        transactionService.create(userId, txnOn(TransactionType.EXPENSE, 120_000, cashId, foodId, "Ăn tối", TODAY.minusDays(1)));
        transactionService.create(userId, txnOn(TransactionType.INCOME, 15_000_000, bankId, salaryId, "Lương", TODAY.minusDays(5)));

        var all = transactionService.search(userId, TransactionFilter.between(TODAY.withDayOfMonth(1), TODAY), 0, 10);
        assertThat(all.page().totalItems()).isEqualTo(3);
        assertThat(all.page().items().get(0).note()).isEqualTo("Cà phê sáng");
        assertThat(all.totalExpense()).isEqualByComparingTo("165000");
        assertThat(all.totalIncome()).isEqualByComparingTo("15000000");

        var byKeyword = transactionService.search(userId,
                new TransactionFilter(null, null, null, null, null, null, null, null, "phê"), 0, 10);
        assertThat(byKeyword.page().items()).extracting(t -> t.note()).containsExactly("Cà phê sáng");

        var byWallet = transactionService.search(userId,
                new TransactionFilter(null, null, bankId, null, null, money(1_000_000), null, TransactionSource.MANUAL, null), 0, 10);
        assertThat(byWallet.page().totalItems()).isEqualTo(1);
    }

    // ---------- UC-A04, B06 ----------

    @Test
    void budgetWarnsOnceThenWarnsOverOnce() {
        BudgetResponse budget = budgetService.create(userId, new BudgetRequest(foodId, "2026-10", money(1_000_000), null));
        assertThat(budget.status()).isEqualTo(BudgetResponse.Status.OK);

        transactionService.create(userId, txn(TransactionType.EXPENSE, 500_000, cashId, null, foodId));
        assertThat(notifications()).isEmpty();

        transactionService.create(userId, txn(TransactionType.EXPENSE, 350_000, cashId, null, foodId));
        List<NotificationResponse> afterWarn = notifications();
        assertThat(afterWarn).hasSize(1);
        assertThat(afterWarn.get(0).type()).isEqualTo(NotificationType.BUDGET_WARN);
        // 20/10 → còn 12 ngày (kể cả hôm nay); 150.000 / 12 = 12.500
        assertThat(afterWarn.get(0).content()).contains("85%", "150.000đ", "12 ngày", "12.500đ/ngày");

        transactionService.create(userId, txn(TransactionType.EXPENSE, 50_000, cashId, null, foodId));
        assertThat(notifications()).hasSize(1);

        transactionService.create(userId, txn(TransactionType.EXPENSE, 200_000, cashId, null, foodId));
        transactionService.create(userId, txn(TransactionType.EXPENSE, 10_000, cashId, null, foodId));
        List<NotificationResponse> all = notifications();
        assertThat(all).extracting(NotificationResponse::type)
                .containsExactlyInAnyOrder(NotificationType.BUDGET_WARN, NotificationType.BUDGET_OVER);

        BudgetResponse after = budgetService.get(userId, budget.id());
        assertThat(after.spent()).isEqualByComparingTo("1110000");
        assertThat(after.status()).isEqualTo(BudgetResponse.Status.OVER);
        assertThat(after.percentUsed()).isEqualTo(111);

        assertThat(notificationService.unreadCount(userId)).isEqualTo(2);
        notificationService.markAllRead(userId);
        assertThat(notificationService.unreadCount(userId)).isZero();

        var copied = budgetService.copyFromPreviousMonth(userId, "2026-11");
        assertThat(copied).singleElement().satisfies(b -> {
            assertThat(b.month()).isEqualTo("2026-11");
            assertThat(b.spent()).isEqualByComparingTo("0");
        });
        assertError(ErrorCode.BUDGET_DUPLICATE,
                () -> budgetService.create(userId, new BudgetRequest(foodId, "2026-10", money(1), null)));
        assertError(ErrorCode.BUDGET_CATEGORY_INVALID,
                () -> budgetService.create(userId, new BudgetRequest(salaryId, "2026-10", money(1), null)));
    }

    @Test
    void parentBudgetCountsChildCategorySpending() {
        CategoryResponse coffee = categoryService.create(userId,
                new CategoryRequest("Cà phê riêng", CategoryType.EXPENSE, null, foodId));
        budgetService.create(userId, new BudgetRequest(foodId, "2026-10", money(100_000), 80));

        transactionService.create(userId, txn(TransactionType.EXPENSE, 90_000, cashId, null, coffee.id()));
        assertThat(notifications()).extracting(NotificationResponse::type).containsExactly(NotificationType.BUDGET_WARN);
    }

    // ---------- UC-A03 ----------

    @Test
    void mergeMovesTransactionsAndBudgetsIntoTarget() {
        CategoryResponse own = categoryService.create(userId,
                new CategoryRequest("Trà sữa", CategoryType.EXPENSE, null, null));
        transactionService.create(userId, txn(TransactionType.EXPENSE, 40_000, cashId, null, own.id()));
        budgetService.create(userId, new BudgetRequest(own.id(), "2026-10", money(300_000), null));
        budgetService.create(userId, new BudgetRequest(foodId, "2026-10", money(2_000_000), null));

        assertError(ErrorCode.CATEGORY_IN_USE, () -> categoryService.delete(userId, own.id()));
        assertError(ErrorCode.CATEGORY_NOT_EDITABLE, () -> categoryService.setHidden(userId, foodId, true));
        assertError(ErrorCode.CATEGORY_NAME_DUPLICATE, () -> categoryService.create(userId,
                new CategoryRequest("ăn uống", CategoryType.EXPENSE, null, null)));

        categoryService.merge(userId, own.id(), new MergeCategoryRequest(foodId));

        var food = transactionService.search(userId,
                new TransactionFilter(null, null, null, foodId, null, null, null, null, null), 0, 10);
        assertThat(food.page().totalItems()).isEqualTo(1);
        List<BudgetResponse> budgets = budgetService.list(userId, "2026-10");
        assertThat(budgets).singleElement().satisfies(b -> {
            assertThat(b.categoryId()).isEqualTo(foodId);
            assertThat(b.limitAmount()).isEqualByComparingTo("2300000");
            assertThat(b.spent()).isEqualByComparingTo("40000");
        });
        assertThat(categoryService.list(userId, CategoryType.EXPENSE, true))
                .extracting(CategoryResponse::name).doesNotContain("Trà sữa");
    }

    // ---------- UC-A05 ----------

    @Test
    void savingGoalDepositWithdrawAndCompletion() {
        var goal = goalService.create(userId, new SavingGoalRequest("Mua laptop", money(1_000_000), LocalDate.of(2027, 1, 20)));
        assertThat(goal.monthlyNeeded()).isEqualByComparingTo("333334");

        goalService.deposit(userId, goal.id(), new WalletAmountRequest(bankId, money(600_000), null, null));
        assertThat(balance(bankId)).isEqualByComparingTo("9400000");
        assertError(ErrorCode.GOAL_WITHDRAW_EXCEEDS,
                () -> goalService.withdraw(userId, goal.id(), new WalletAmountRequest(bankId, money(700_000), null, null)));

        var done = goalService.deposit(userId, goal.id(), new WalletAmountRequest(bankId, money(400_000), null, null));
        assertThat(done.status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(done.percent()).isEqualTo(100);
        assertThat(notifications()).extracting(NotificationResponse::type).contains(NotificationType.GOAL_COMPLETED);

        var linked = goalService.transactions(userId, goal.id());
        assertThat(linked).hasSize(2);
        assertError(ErrorCode.TRANSACTION_LINKED, () -> transactionService.delete(userId, linked.get(0).id()));

        var back = goalService.withdraw(userId, goal.id(), new WalletAmountRequest(bankId, money(100_000), null, null));
        assertThat(back.status()).isEqualTo(GoalStatus.IN_PROGRESS);
        assertThat(balance(bankId)).isEqualByComparingTo("9100000");
    }

    // ---------- UC-A06 ----------

    @Test
    void debtLifecycle() {
        var debt = debtService.create(userId, new DebtCreateRequest(DebtType.BORROW, "Anh Nam", money(5_000_000),
                bankId, null, TODAY.plusDays(3), null));
        assertThat(balance(bankId)).isEqualByComparingTo("15000000");

        debtService.recordPayment(userId, debt.id(), new WalletAmountRequest(bankId, money(2_000_000), null, null));
        assertError(ErrorCode.DEBT_PAYMENT_EXCEEDS, () -> debtService.recordPayment(userId, debt.id(),
                new WalletAmountRequest(bankId, money(4_000_000), null, null)));

        var summary = debtService.summary(userId);
        assertThat(summary.totalBorrowing()).isEqualByComparingTo("3000000");
        assertThat(summary.dueSoon()).hasSize(1);

        // UC-D04: hạn trả sau đúng 3 ngày → nhắc
        reminderJob.runFor(TODAY);
        assertThat(notifications()).extracting(NotificationResponse::type).contains(NotificationType.DEBT_DUE);

        var paid = debtService.recordPayment(userId, debt.id(), new WalletAmountRequest(bankId, money(3_000_000), null, null));
        assertThat(paid.status()).isEqualTo(DebtStatus.PAID);
        assertThat(balance(bankId)).isEqualByComparingTo("10000000");

        var lend = debtService.create(userId, new DebtCreateRequest(DebtType.LEND, "Bạn Hoa", money(1_000_000),
                cashId, null, null, null));
        assertThat(balance(cashId)).isEqualByComparingTo("-1000000");
        debtService.delete(userId, lend.id());
        assertThat(balance(cashId)).isEqualByComparingTo("0");
    }

    // ---------- UC-A07, D04 ----------

    @Test
    void recurringRulesAutoCreateAndRemind() {
        var rent = ruleService.create(userId, new RecurringRuleRequest(TransactionType.EXPENSE, money(3_000_000),
                bankId, category("Nhà ở", CategoryType.EXPENSE), "Tiền nhà", RecurrenceFrequency.MONTHLY,
                LocalDate.of(2026, 8, 31), true));
        assertThat(rent.nextRunDate()).isEqualTo("2026-10-31");

        var salary = ruleService.create(userId, new RecurringRuleRequest(TransactionType.INCOME, money(15_000_000),
                bankId, salaryId, "Lương tháng", RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 10, 5), false));
        assertThat(salary.nextRunDate()).isEqualTo("2026-11-05");

        reminderJob.runFor(LocalDate.of(2026, 10, 31));
        assertThat(ruleService.get(userId, rent.id()).nextRunDate()).isEqualTo("2026-11-30");
        assertThat(balance(bankId)).isEqualByComparingTo("7000000");
        var recurring = transactionService.search(userId,
                new TransactionFilter(null, null, null, null, null, null, null, TransactionSource.RECURRING, null), 0, 10);
        assertThat(recurring.page().items()).singleElement()
                .satisfies(t -> assertThat(t.txnDate()).isEqualTo("2026-10-31"));

        // Chạy lại cùng ngày không ghi trùng
        reminderJob.runFor(LocalDate.of(2026, 10, 31));
        assertThat(balance(bankId)).isEqualByComparingTo("7000000");

        // Quy tắc nhắc (không tự tạo): chỉ gửi thông báo, không đổi số dư
        reminderJob.runFor(LocalDate.of(2026, 11, 5));
        assertThat(balance(bankId)).isEqualByComparingTo("7000000");
        assertThat(ruleService.get(userId, salary.id()).nextRunDate()).isEqualTo("2026-12-05");
        assertThat(notifications()).filteredOn(n -> n.type() == NotificationType.RECURRING)
                .extracting(NotificationResponse::content)
                .anySatisfy(c -> assertThat(c).contains("Lương tháng", "xác nhận"));

        ruleService.execute(userId, salary.id(), LocalDate.of(2026, 11, 5));
        assertThat(balance(bankId)).isEqualByComparingTo("22000000");

        ruleService.setActive(userId, rent.id(), false);
        reminderJob.runFor(LocalDate.of(2026, 11, 30));
        assertThat(balance(bankId)).isEqualByComparingTo("22000000");
    }

    // ---------- UC-A09, A10 ----------

    @Test
    void reportOverviewAndNetWorth() {
        transactionService.create(userId, txnOn(TransactionType.INCOME, 15_000_000, bankId, salaryId, null, TODAY.minusDays(10)));
        transactionService.create(userId, txnOn(TransactionType.EXPENSE, 600_000, cashId, foodId, null, TODAY));
        transactionService.create(userId, txnOn(TransactionType.EXPENSE, 400_000, bankId,
                category("Di chuyển", CategoryType.EXPENSE), null, TODAY));
        transactionService.create(userId, txnOn(TransactionType.EXPENSE, 200_000, cashId, foodId, null, LocalDate.of(2026, 9, 15)));
        debtService.create(userId, new DebtCreateRequest(DebtType.LEND, "Bạn A", money(1_000_000), bankId, TODAY, null, null));

        var report = reportService.overview(userId, null, null);
        assertThat(report.from()).isEqualTo("2026-10-01");
        assertThat(report.totalIncome()).isEqualByComparingTo("15000000");
        assertThat(report.totalExpense()).isEqualByComparingTo("1000000"); // không tính khoản cho vay
        assertThat(report.topExpenseCategories().get(0).categoryName()).isEqualTo("Ăn uống");
        assertThat(report.topExpenseCategories().get(0).percent()).isEqualTo(60.0);

        assertThat(report.monthlyTrend()).hasSize(6);
        assertThat(report.monthlyTrend().get(4).month()).isEqualTo("2026-09");
        assertThat(report.monthlyTrend().get(4).expense()).isEqualByComparingTo("200000");

        // Ví: tiền mặt −800.000, ngân hàng 10tr + 15tr − 400k − 1tr cho vay = 23,6tr
        assertThat(report.netWorth().walletBalance()).isEqualByComparingTo("22800000");
        assertThat(report.netWorth().lending()).isEqualByComparingTo("1000000");
        assertThat(report.netWorth().netWorth()).isEqualByComparingTo("23800000");
        assertThat(report.balanceHistory()).hasSize(6);
        assertThat(report.balanceHistory().get(5).totalBalance()).isEqualByComparingTo("22800000");
        assertThat(report.balanceHistory().get(4).totalBalance()).isEqualByComparingTo("9800000");

        byte[] csv = exportService.exportTransactions(userId, TODAY.withDayOfMonth(1), TODAY, ExportService.Format.CSV);
        String text = new String(csv, StandardCharsets.UTF_8);
        assertThat(text).startsWith("﻿Ngày,Loại,Danh mục").contains("Khoản nợ", "Ăn uống", "600000");

        byte[] xlsx = exportService.exportTransactions(userId, TODAY.withDayOfMonth(1), TODAY, ExportService.Format.XLSX);
        assertThat(new String(xlsx, 0, 2, StandardCharsets.US_ASCII)).isEqualTo("PK");

        assertError(ErrorCode.NO_DATA_TO_EXPORT, () -> exportService.exportTransactions(userId,
                LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31), ExportService.Format.CSV));
    }

    // ---------- UC-B04 ----------

    @Test
    void riskAssessment() {
        var p = profileService.submitRiskAnswers(userId, new RiskAnswersRequest(List.of(3, 3, 3, 2, 2)));
        assertThat(p.riskScore()).isEqualTo(13);
        assertThat(p.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(profileService.submitRiskAnswers(userId, new RiskAnswersRequest(List.of(1, 1, 2, 2, 2))).riskLevel())
                .isEqualTo(RiskLevel.LOW);
        assertError(ErrorCode.RISK_ANSWERS_INVALID,
                () -> profileService.submitRiskAnswers(userId, new RiskAnswersRequest(List.of(1, 2, 3))));
    }

    // ---------- helpers ----------

    private Long category(String name, CategoryType type) {
        return categoryService.list(userId, type, false).stream()
                .filter(c -> c.name().equals(name)).findFirst().orElseThrow().id();
    }

    private BigDecimal balance(Long walletId) {
        return walletService.get(userId, walletId).currentBalance();
    }

    private List<NotificationResponse> notifications() {
        return notificationService.list(userId, false, 0, 50).items();
    }

    private static TransactionRequest txn(TransactionType type, long amount, Long walletId, Long toWalletId, Long categoryId) {
        return new TransactionRequest(type, money(amount), walletId, toWalletId, categoryId, TODAY, null);
    }

    private static TransactionRequest txnOn(TransactionType type, long amount, Long walletId, Long categoryId,
                                            String note, LocalDate date) {
        return new TransactionRequest(type, money(amount), walletId, null, categoryId, date, note);
    }

    private static BigDecimal money(long value) {
        return BigDecimal.valueOf(value);
    }

    private static void assertError(ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", code);
    }
}
