package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Transaction;
import com.fintech.chatbot.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * DAO cho E-04 Transaction (UC-A02, A04, A08, A09, A10).
 */
public interface TransactionDao extends UserOwnedDao<Transaction, Long> {

    /** UC-A08: lọc + phân trang, mới nhất trước; nạp sẵn ví và danh mục. */
    List<Transaction> search(Long userId, TransactionFilter filter, int page, int size);

    long countSearch(Long userId, TransactionFilter filter);

    /** Toàn bộ kết quả lọc, không phân trang (UC-A10 xuất file). */
    List<Transaction> findAll(Long userId, TransactionFilter filter);

    /** Tổng thu / tổng chi của tập kết quả lọc (chỉ tính INCOME, EXPENSE). */
    IncomeExpense totals(Long userId, TransactionFilter filter);

    /** BR-07: biến động số dư của một ví = thu − chi − chuyển đi + chuyển đến ± điều chỉnh. */
    BigDecimal netChangeForWallet(Long walletId);

    /** Biến động tổng tài sản của user đến hết ngày {@code to} (chuyển tiền nội bộ triệt tiêu nhau). */
    BigDecimal netChangeForUserUntil(Long userId, LocalDate to);

    boolean existsByWallet(Long walletId);

    /** Giao dịch gắn một khoản nợ (UC-A06), mới nhất trước. */
    List<Transaction> findByDebt(Long userId, Long debtId);

    /** Giao dịch gắn một mục tiêu tiết kiệm (UC-A05), mới nhất trước. */
    List<Transaction> findByGoal(Long userId, Long goalId);

    /** Tổng chi của các danh mục trong khoảng ngày (UC-A04, B06). */
    BigDecimal sumExpense(Long userId, Collection<Long> categoryIds, LocalDate from, LocalDate to);

    /**
     * Tổng theo danh mục của một loại giao dịch, giảm dần (UC-A09).
     * Bỏ qua giao dịch gắn khoản nợ / mục tiêu tiết kiệm vì đó là dịch chuyển tiền, không phải thu chi.
     */
    List<CategoryAmount> sumByCategory(Long userId, TransactionType type, LocalDate from, LocalDate to);

    /** Tổng thu, chi theo từng tháng (UC-A09), bỏ qua giao dịch gắn nợ / mục tiêu. */
    List<MonthAmount> monthlyTotals(Long userId, LocalDate from, LocalDate to);

    /** Chuyển toàn bộ giao dịch từ danh mục này sang danh mục khác (UC-A03 AF-3 gộp danh mục). */
    int reassignCategory(Long userId, Long fromCategoryId, Long toCategoryId);

    record IncomeExpense(BigDecimal income, BigDecimal expense) {
    }

    record CategoryAmount(Long categoryId, String categoryName, BigDecimal amount) {
    }

    record MonthAmount(int year, int month, BigDecimal income, BigDecimal expense) {
    }
}
