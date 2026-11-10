package com.fintech.chatbot.service.support;

import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dao.WalletDao;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.Debt;
import com.fintech.chatbot.entity.SavingGoal;
import com.fintech.chatbot.entity.Transaction;
import com.fintech.chatbot.entity.Wallet;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Điểm ghi giao dịch dùng chung cho mọi luồng (form, nợ, mục tiêu, định kỳ, và chat / giọng nói ở giai đoạn 3).
 * Mỗi lần ghi / sửa / xóa đều tính lại số dư các ví liên quan (BR-07) và kiểm tra cảnh báo ngân sách (UC-B06).
 */
@Component
@RequiredArgsConstructor
public class TransactionRecorder {

    private final TransactionDao transactionDao;
    private final WalletDao walletDao;
    private final BudgetAlertService budgetAlertService;

    /** Dữ liệu một giao dịch; các trường quan hệ đã được kiểm tra quyền sở hữu trước khi truyền vào. */
    @Builder
    public record Draft(
            TransactionType type,
            BigDecimal amount,
            Wallet wallet,
            Wallet toWallet,
            Category category,
            LocalDate date,
            String note,
            TransactionSource source,
            Debt debt,
            SavingGoal goal
    ) {
    }

    public Transaction record(Long userId, Draft draft) {
        Transaction t = new Transaction();
        t.setUserId(userId);
        apply(t, draft);
        t.setSource(draft.source() == null ? TransactionSource.MANUAL : draft.source());
        t.assignDebt(draft.debt());
        t.assignGoal(draft.goal());
        transactionDao.save(t);
        afterChange(t, Set.of());
        return t;
    }

    /** Sửa giao dịch; tính lại cả ví cũ nếu đổi ví. */
    public Transaction update(Transaction t, Draft draft) {
        Set<Wallet> before = walletsOf(t);
        apply(t, draft);
        afterChange(t, before);
        return t;
    }

    public void remove(Transaction t) {
        Set<Wallet> wallets = walletsOf(t);
        transactionDao.delete(t);
        transactionDao.flush();
        wallets.forEach(this::recalculate);
    }

    /** BR-07: số dư = số dư ban đầu + biến động từ mọi giao dịch của ví. */
    public BigDecimal recalculate(Wallet wallet) {
        transactionDao.flush();
        BigDecimal balance = wallet.getInitialBalance().add(transactionDao.netChangeForWallet(wallet.getId()));
        wallet.setCurrentBalance(balance);
        walletDao.save(wallet);
        return balance;
    }

    private void apply(Transaction t, Draft d) {
        t.setType(d.type());
        t.setAmount(d.amount());
        t.assignWallet(d.wallet());
        t.assignToWallet(d.toWallet());
        t.assignCategory(d.category());
        t.setTxnDate(Objects.requireNonNull(d.date(), "date"));
        t.setNote(d.note() == null || d.note().isBlank() ? null : d.note().trim());
    }

    private void afterChange(Transaction t, Set<Wallet> previousWallets) {
        Set<Wallet> wallets = new LinkedHashSet<>(previousWallets);
        wallets.addAll(walletsOf(t));
        wallets.forEach(this::recalculate);
        if (t.getType() == TransactionType.EXPENSE) {
            budgetAlertService.checkAfterExpense(t.getUserId(), t.getCategoryId(), t.getTxnDate());
        }
    }

    private static Set<Wallet> walletsOf(Transaction t) {
        Set<Wallet> wallets = new LinkedHashSet<>();
        if (t.getWallet() != null) {
            wallets.add(t.getWallet());
        }
        if (t.getToWallet() != null) {
            wallets.add(t.getToWallet());
        }
        return wallets;
    }
}
