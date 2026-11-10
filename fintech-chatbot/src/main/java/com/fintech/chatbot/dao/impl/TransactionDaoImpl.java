package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dao.TransactionFilter;
import com.fintech.chatbot.entity.Transaction;
import com.fintech.chatbot.entity.enums.TransactionType;
import org.hibernate.query.SelectionQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Repository
public class TransactionDaoImpl extends AbstractUserOwnedDao<Transaction> implements TransactionDao {

    /** Điều kiện loại trừ giao dịch gắn nợ / mục tiêu khỏi báo cáo thu chi. */
    private static final String NOT_LINKED = " and t.debtId is null and t.goalId is null";

    public TransactionDaoImpl() {
        super(Transaction.class);
    }

    @Override
    public List<Transaction> search(Long userId, TransactionFilter filter, int page, int size) {
        Where where = where(userId, filter);
        SelectionQuery<Transaction> q = query("""
                select t from Transaction t
                join fetch t.wallet
                left join fetch t.toWallet
                left join fetch t.category
                """ + where.clause + " order by t.txnDate desc, t.id desc");
        where.bind(q);
        return page(q, page, size);
    }

    @Override
    public long countSearch(Long userId, TransactionFilter filter) {
        Where where = where(userId, filter);
        SelectionQuery<Long> q = session().createSelectionQuery(
                "select count(t) from Transaction t" + where.clause, Long.class);
        where.bind(q);
        return q.getSingleResult();
    }

    @Override
    public List<Transaction> findAll(Long userId, TransactionFilter filter) {
        Where where = where(userId, filter);
        SelectionQuery<Transaction> q = query("""
                select t from Transaction t
                join fetch t.wallet
                left join fetch t.toWallet
                left join fetch t.category
                """ + where.clause + " order by t.txnDate desc, t.id desc");
        where.bind(q);
        return q.getResultList();
    }

    @Override
    public IncomeExpense totals(Long userId, TransactionFilter filter) {
        Where where = where(userId, filter);
        SelectionQuery<Object[]> q = session().createSelectionQuery(
                "select t.type, sum(t.amount) from Transaction t" + where.clause
                        + " and t.type in (:incomeType, :expenseType) group by t.type", Object[].class);
        where.bind(q);
        q.setParameter("incomeType", TransactionType.INCOME).setParameter("expenseType", TransactionType.EXPENSE);
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (Object[] row : q.getResultList()) {
            if (row[0] == TransactionType.INCOME) {
                income = (BigDecimal) row[1];
            } else {
                expense = (BigDecimal) row[1];
            }
        }
        return new IncomeExpense(income, expense);
    }

    @Override
    public BigDecimal netChangeForWallet(Long walletId) {
        BigDecimal result = session().createSelectionQuery("""
                        select sum(case
                            when t.walletId = :w and t.type = :income then t.amount
                            when t.walletId = :w and t.type = :adjust then t.amount
                            when t.walletId = :w then -t.amount
                            when t.toWalletId = :w then t.amount
                        end)
                        from Transaction t
                        where t.walletId = :w or t.toWalletId = :w
                        """, BigDecimal.class)
                .setParameter("w", walletId)
                .setParameter("income", TransactionType.INCOME)
                .setParameter("adjust", TransactionType.ADJUST)
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }

    @Override
    public BigDecimal netChangeForUserUntil(Long userId, LocalDate to) {
        BigDecimal result = session().createSelectionQuery("""
                        select sum(case
                            when t.type = :income then t.amount
                            when t.type = :adjust then t.amount
                            when t.type = :expense then -t.amount
                        end)
                        from Transaction t
                        where t.userId = :userId and t.txnDate <= :to
                        """, BigDecimal.class)
                .setParameter("userId", userId)
                .setParameter("to", to)
                .setParameter("income", TransactionType.INCOME)
                .setParameter("adjust", TransactionType.ADJUST)
                .setParameter("expense", TransactionType.EXPENSE)
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }

    @Override
    public boolean existsByWallet(Long walletId) {
        return session().createSelectionQuery(
                        "select count(t) from Transaction t where t.walletId = :w or t.toWalletId = :w", Long.class)
                .setParameter("w", walletId)
                .getSingleResult() > 0;
    }

    @Override
    public List<Transaction> findByDebt(Long userId, Long debtId) {
        return query("""
                select t from Transaction t join fetch t.wallet
                where t.userId = :userId and t.debtId = :debtId
                order by t.txnDate desc, t.id desc
                """)
                .setParameter("userId", userId)
                .setParameter("debtId", debtId)
                .getResultList();
    }

    @Override
    public List<Transaction> findByGoal(Long userId, Long goalId) {
        return query("""
                select t from Transaction t join fetch t.wallet
                where t.userId = :userId and t.goalId = :goalId
                order by t.txnDate desc, t.id desc
                """)
                .setParameter("userId", userId)
                .setParameter("goalId", goalId)
                .getResultList();
    }

    @Override
    public BigDecimal sumExpense(Long userId, Collection<Long> categoryIds, LocalDate from, LocalDate to) {
        if (categoryIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = session().createSelectionQuery("""
                        select sum(t.amount) from Transaction t
                        where t.userId = :userId
                          and t.type = :expense
                          and t.categoryId in (:categoryIds)
                          and t.txnDate between :from and :to
                        """, BigDecimal.class)
                .setParameter("userId", userId)
                .setParameter("expense", TransactionType.EXPENSE)
                .setParameterList("categoryIds", categoryIds)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }

    @Override
    public List<CategoryAmount> sumByCategory(Long userId, TransactionType type, LocalDate from, LocalDate to) {
        return session().createSelectionQuery("""
                        select c.id, c.name, sum(t.amount)
                        from Transaction t join t.category c
                        where t.userId = :userId
                          and t.type = :type
                          and t.txnDate between :from and :to
                        """ + NOT_LINKED + """
                         group by c.id, c.name
                         order by sum(t.amount) desc
                        """, Object[].class)
                .setParameter("userId", userId)
                .setParameter("type", type)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList()
                .stream()
                .map(r -> new CategoryAmount((Long) r[0], (String) r[1], (BigDecimal) r[2]))
                .toList();
    }

    @Override
    public List<MonthAmount> monthlyTotals(Long userId, LocalDate from, LocalDate to) {
        List<Object[]> rows = session().createSelectionQuery("""
                        select extract(year from t.txnDate), extract(month from t.txnDate), t.type, sum(t.amount)
                        from Transaction t
                        where t.userId = :userId
                          and t.type in (:income, :expense)
                          and t.txnDate between :from and :to
                        """ + NOT_LINKED + """
                         group by extract(year from t.txnDate), extract(month from t.txnDate), t.type
                        """, Object[].class)
                .setParameter("userId", userId)
                .setParameter("income", TransactionType.INCOME)
                .setParameter("expense", TransactionType.EXPENSE)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        Map<Integer, BigDecimal[]> byMonth = new TreeMap<>();
        for (Object[] r : rows) {
            int year = ((Number) r[0]).intValue();
            int month = ((Number) r[1]).intValue();
            BigDecimal[] pair = byMonth.computeIfAbsent(year * 100 + month,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            pair[r[2] == TransactionType.INCOME ? 0 : 1] = (BigDecimal) r[3];
        }
        List<MonthAmount> result = new ArrayList<>();
        byMonth.forEach((key, pair) -> result.add(new MonthAmount(key / 100, key % 100, pair[0], pair[1])));
        return result;
    }

    @Override
    public int reassignCategory(Long userId, Long fromCategoryId, Long toCategoryId) {
        return session().createMutationQuery("""
                        update Transaction t set t.category.id = :toId
                        where t.userId = :userId and t.category.id = :fromId
                        """)
                .setParameter("toId", toCategoryId)
                .setParameter("userId", userId)
                .setParameter("fromId", fromCategoryId)
                .executeUpdate();
    }

    // ---------- Dựng điều kiện lọc động ----------

    private static Where where(Long userId, TransactionFilter f) {
        Where w = new Where();
        w.add("t.userId = :userId", "userId", userId);
        if (f == null) {
            return w;
        }
        if (f.from() != null) {
            w.add("t.txnDate >= :from", "from", f.from());
        }
        if (f.to() != null) {
            w.add("t.txnDate <= :to", "to", f.to());
        }
        if (f.walletId() != null) {
            w.add("(t.walletId = :walletId or t.toWalletId = :walletId)", "walletId", f.walletId());
        }
        if (f.categoryId() != null) {
            w.add("t.categoryId = :categoryId", "categoryId", f.categoryId());
        }
        if (f.type() != null) {
            w.add("t.type = :type", "type", f.type());
        }
        if (f.minAmount() != null) {
            w.add("t.amount >= :minAmount", "minAmount", f.minAmount());
        }
        if (f.maxAmount() != null) {
            w.add("t.amount <= :maxAmount", "maxAmount", f.maxAmount());
        }
        if (f.source() != null) {
            w.add("t.source = :source", "source", f.source());
        }
        if (f.keyword() != null && !f.keyword().isBlank()) {
            w.add("lower(t.note) like :keyword", "keyword",
                    "%" + f.keyword().trim().toLowerCase(Locale.ROOT) + "%");
        }
        return w;
    }

    private static final class Where {
        private final List<String> conditions = new ArrayList<>();
        private final Map<String, Object> params = new HashMap<>();
        private String clause = "";

        void add(String condition, String param, Object value) {
            conditions.add(condition);
            params.put(param, value);
            clause = " where " + String.join(" and ", conditions);
        }

        void bind(SelectionQuery<?> q) {
            params.forEach(q::setParameter);
        }
    }
}
