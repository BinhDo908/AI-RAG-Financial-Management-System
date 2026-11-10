package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.DebtDao;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dao.WalletDao;
import com.fintech.chatbot.dto.response.ReportOverviewResponse;
import com.fintech.chatbot.dto.response.ReportOverviewResponse.BalancePoint;
import com.fintech.chatbot.dto.response.ReportOverviewResponse.CategoryShare;
import com.fintech.chatbot.dto.response.ReportOverviewResponse.MonthlyTotal;
import com.fintech.chatbot.dto.response.ReportOverviewResponse.NetWorth;
import com.fintech.chatbot.entity.Wallet;
import com.fintech.chatbot.entity.enums.DebtType;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final int TREND_MONTHS = 6;
    private static final int TOP_CATEGORIES = 5;

    private final TransactionDao transactionDao;
    private final WalletDao walletDao;
    private final DebtDao debtDao;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ReportOverviewResponse overview(Long userId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from == null ? today.withDayOfMonth(1) : from;
        LocalDate end = to == null ? today : to;
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        // (a) Tổng thu, chi — không tính chuyển tiền, điều chỉnh, giao dịch gắn nợ / mục tiêu
        List<TransactionDao.CategoryAmount> expenses =
                transactionDao.sumByCategory(userId, TransactionType.EXPENSE, start, end);
        BigDecimal totalExpense = sum(expenses);
        BigDecimal totalIncome = sum(transactionDao.sumByCategory(userId, TransactionType.INCOME, start, end));

        // (b), (e) Chi theo danh mục và top 5
        List<CategoryShare> shares = expenses.stream()
                .map(c -> new CategoryShare(c.categoryId(), c.categoryName(), c.amount(), percent(c.amount(), totalExpense)))
                .toList();

        return new ReportOverviewResponse(start, end, totalIncome, totalExpense, totalIncome.subtract(totalExpense),
                shares, shares.stream().limit(TOP_CATEGORIES).toList(),
                monthlyTrend(userId, end), balanceHistory(userId, end), netWorth(userId));
    }

    /** (c) Thu – chi 6 tháng gần nhất tính đến tháng chứa {@code end}, đủ 6 tháng kể cả tháng không có dữ liệu. */
    private List<MonthlyTotal> monthlyTrend(Long userId, LocalDate end) {
        YearMonth last = YearMonth.from(end);
        YearMonth first = last.minusMonths(TREND_MONTHS - 1L);
        Map<YearMonth, TransactionDao.MonthAmount> byMonth = new HashMap<>();
        for (var m : transactionDao.monthlyTotals(userId, first.atDay(1), last.atEndOfMonth())) {
            byMonth.put(YearMonth.of(m.year(), m.month()), m);
        }
        List<MonthlyTotal> result = new ArrayList<>();
        for (YearMonth ym = first; !ym.isAfter(last); ym = ym.plusMonths(1)) {
            var m = byMonth.get(ym);
            result.add(new MonthlyTotal(ym.toString(),
                    m == null ? BigDecimal.ZERO : m.income(), m == null ? BigDecimal.ZERO : m.expense()));
        }
        return result;
    }

    /**
     * (d) Tổng số dư mọi ví tại cuối mỗi tháng trong 6 tháng (tháng cuối tính đến {@code end}).
     * Số dư tại ngày D = Σ số dư ban đầu + biến động của mọi giao dịch đến hết ngày D.
     */
    private List<BalancePoint> balanceHistory(Long userId, LocalDate end) {
        BigDecimal initialTotal = walletDao.findAllByUserId(userId).stream()
                .map(Wallet::getInitialBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        YearMonth last = YearMonth.from(end);
        List<BalancePoint> points = new ArrayList<>();
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth ym = last.minusMonths(i);
            LocalDate date = i == 0 ? end : ym.atEndOfMonth();
            points.add(new BalancePoint(date, initialTotal.add(transactionDao.netChangeForUserUntil(userId, date))));
        }
        return points;
    }

    /** (f) Tài sản ròng = Σ số dư ví (kể cả ví lưu trữ) + cho vay còn lại − đi vay còn lại. */
    private NetWorth netWorth(Long userId) {
        BigDecimal wallets = walletDao.findAllByUserId(userId).stream()
                .map(Wallet::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lending = debtDao.sumRemaining(userId, DebtType.LEND);
        BigDecimal borrowing = debtDao.sumRemaining(userId, DebtType.BORROW);
        return new NetWorth(wallets, lending, borrowing, wallets.add(lending).subtract(borrowing));
    }

    private static BigDecimal sum(List<TransactionDao.CategoryAmount> rows) {
        return rows.stream().map(TransactionDao.CategoryAmount::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static double percent(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP).doubleValue();
    }
}
