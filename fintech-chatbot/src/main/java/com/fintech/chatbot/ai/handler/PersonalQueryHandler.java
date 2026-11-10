package com.fintech.chatbot.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintech.chatbot.ai.AiSettings;
import com.fintech.chatbot.ai.AiTask;
import com.fintech.chatbot.ai.Intent;
import com.fintech.chatbot.ai.JsonSupport;
import com.fintech.chatbot.ai.Prompts;
import com.fintech.chatbot.ai.ReplySink;
import com.fintech.chatbot.ai.client.ChatMessage;
import com.fintech.chatbot.ai.client.LlmClient;
import com.fintech.chatbot.common.util.MoneyFormatter;
import com.fintech.chatbot.dao.AiDataPermissionDao;
import com.fintech.chatbot.dao.CategoryDao;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dao.TransactionFilter;
import com.fintech.chatbot.dao.WalletDao;
import com.fintech.chatbot.entity.AiDataPermission;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.Transaction;
import com.fintech.chatbot.entity.Wallet;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.service.BudgetService;
import com.fintech.chatbot.service.DebtService;
import com.fintech.chatbot.service.SavingGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * UC-A13 — Hỏi đáp dữ liệu tài chính cá nhân.
 *
 * <p>LLM chỉ sinh truy vấn JSON theo schema cố định (không sinh SQL). Code kiểm tra schema, luôn lọc theo
 * userId của người đang đăng nhập (BR-05), tôn trọng quyền dữ liệu AI (UC-D05), tự tính mọi con số (BR-06),
 * rồi LLM chỉ diễn đạt kết quả.
 */
@Component
@RequiredArgsConstructor
public class PersonalQueryHandler {

    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final Set<String> ENTITIES = Set.of("transaction", "wallet", "budget", "debt", "goal");
    private static final Set<String> METRICS = Set.of("sum", "count", "avg", "list", "balance");
    private static final Set<String> GROUPS = Set.of("category", "wallet", "month");
    private static final int DEFAULT_DAYS = 30;
    private static final int LIST_LIMIT = 10;

    private final LlmClient llm;
    private final JsonSupport json;
    private final TransactionDao transactionDao;
    private final WalletDao walletDao;
    private final CategoryDao categoryDao;
    private final AiDataPermissionDao permissionDao;
    private final BudgetService budgetService;
    private final DebtService debtService;
    private final SavingGoalService goalService;
    private final Clock clock;

    /** Truy vấn đã kiểm tra hợp lệ. */
    record Plan(String entity, String metric, TransactionType type, Category category, Wallet wallet,
                LocalDate from, LocalDate to, String groupBy, boolean comparePrevious, String assumption) {
    }

    public HandlerResult handle(Long userId, String question, List<ChatMessage> history, AiSettings.Snapshot settings,
                                ReplySink sink) {
        LocalDate today = LocalDate.now(clock);
        List<Category> categories = categoryDao.findVisibleForUser(userId, null);
        List<Wallet> wallets = walletDao.findAllByUserId(userId);

        String system = Prompts.PLAN_QUERY.formatted(today, today.getDayOfWeek().getDisplayName(TextStyle.FULL, VI),
                String.join(", ", categories.stream().map(Category::getName).distinct().toList()),
                String.join(", ", wallets.stream().map(Wallet::getName).toList()));
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(Prompts.withHistory(system, history)));
        messages.add(ChatMessage.user(question));
        JsonNode raw = json.extractObject(llm.chat(settings.model(AiTask.EXTRACTION), messages, 0)).orElse(null);
        if (raw == null || !ENTITIES.contains(raw.path("entity").asText(""))) {
            // EX-3: không dựng được truy vấn hợp lệ
            return HandlerResult.text(Intent.QUERY_PERSONAL_DATA,
                    "Mình chưa hiểu câu hỏi, bạn diễn đạt lại giúp mình nhé? Ví dụ: \"Tháng này tôi chi bao nhiêu cho ăn uống?\"");
        }
        Plan plan = validate(raw, categories, wallets, today);

        String denied = permissionDenied(userId, plan.entity());
        if (denied != null) {
            return HandlerResult.text(Intent.QUERY_PERSONAL_DATA,
                    "Bạn đang tắt quyền cho AI xem dữ liệu %s. Bật trong Cài đặt → Quyền dữ liệu AI nhé.".formatted(denied));
        }

        ObjectNode result = execute(userId, plan);
        sink.stage(ReplySink.Stage.WRITING);
        String phrased = KnowledgeAnswerer.generate(llm, settings.model(AiTask.ANSWER), List.of(
                ChatMessage.system(Prompts.PHRASE_DATA.formatted(result.toPrettyString())),
                ChatMessage.user(question)), settings.temperature(), sink);

        ObjectNode payload = json.object();
        payload.put("type", "PERSONAL_DATA");
        payload.set("result", result);
        return HandlerResult.withPayload(Intent.QUERY_PERSONAL_DATA, phrased, payload);
    }

    // ---------- Kiểm tra truy vấn ----------

    Plan validate(JsonNode raw, List<Category> categories, List<Wallet> wallets, LocalDate today) {
        String entity = raw.path("entity").asText();
        String metric = METRICS.contains(raw.path("metric").asText("")) ? raw.path("metric").asText() : "sum";
        String typeText = JsonSupport.text(raw, "type");
        TransactionType type = "INCOME".equalsIgnoreCase(typeText) ? TransactionType.INCOME
                : "EXPENSE".equalsIgnoreCase(typeText) ? TransactionType.EXPENSE : null;
        Category category = byName(JsonSupport.text(raw, "category"), type, categories);
        Wallet wallet = wallets.stream().filter(w -> w.getName().equalsIgnoreCase(String.valueOf(JsonSupport.text(raw, "wallet"))))
                .findFirst().orElse(null);
        String groupBy = GROUPS.contains(String.valueOf(JsonSupport.text(raw, "group_by"))) ? JsonSupport.text(raw, "group_by") : null;

        LocalDate from = date(JsonSupport.text(raw, "date_from"));
        LocalDate to = date(JsonSupport.text(raw, "date_to"));
        String assumption = null;
        if (from == null && to == null) {
            // AF-1: không rõ thời gian → 30 ngày gần nhất, nói rõ giả định
            to = today;
            from = today.minusDays(DEFAULT_DAYS - 1L);
            assumption = "Không nói rõ thời gian nên tính trong 30 ngày gần nhất (%s – %s)".formatted(from, to);
        } else if (from == null) {
            from = to.withDayOfMonth(1);
        } else if (to == null) {
            to = today;
        }
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        return new Plan(entity, metric, type, category, wallet, from, to, groupBy,
                raw.path("compare_previous").asBoolean(false), assumption);
    }

    private String permissionDenied(Long userId, String entity) {
        AiDataPermission p = permissionDao.findByUserId(userId).orElse(null);
        if (p == null) {
            return null;
        }
        return switch (entity) {
            case "transaction", "wallet" -> p.isAllowTransactions() ? null : "giao dịch";
            case "budget" -> p.isAllowBudgets() ? null : "ngân sách";
            case "debt" -> p.isAllowDebts() ? null : "khoản nợ";
            case "goal" -> p.isAllowGoals() ? null : "mục tiêu tiết kiệm";
            default -> null;
        };
    }

    // ---------- Thực thi (mọi số liệu do code tính) ----------

    ObjectNode execute(Long userId, Plan plan) {
        ObjectNode r = json.object();
        if (plan.assumption() != null) {
            r.put("assumption", plan.assumption());
        }
        switch (plan.entity()) {
            case "wallet" -> wallets(userId, r);
            case "budget" -> r.set("budgets", json.toTree(budgetService.list(userId, YearMonth.from(plan.from()).toString())
                    .stream().map(b -> Map.of("category", b.categoryName(), "month", b.month(),
                            "limit", money(b.limitAmount()), "spent", money(b.spent()), "remaining", money(b.remaining()),
                            "percentUsed", b.percentUsed() + "%", "status", b.status().name())).toList()));
            case "debt" -> {
                var s = debtService.summary(userId);
                r.put("totalBorrowingRemaining", money(s.totalBorrowing()));
                r.put("totalLendingRemaining", money(s.totalLending()));
                r.set("dueWithin7Days", json.toTree(s.dueSoon().stream().map(d -> Map.of(
                        "counterparty", d.counterparty(), "type", d.type().name(), "remaining", money(d.remaining()),
                        "dueDate", String.valueOf(d.dueDate()))).toList()));
            }
            case "goal" -> r.set("goals", json.toTree(goalService.list(userId).stream().map(g -> Map.of(
                    "name", g.name(), "target", money(g.targetAmount()), "current", money(g.currentAmount()),
                    "percent", g.percent() + "%", "status", g.status().name())).toList()));
            default -> transactions(userId, plan, r);
        }
        return r;
    }

    private void wallets(Long userId, ObjectNode r) {
        List<Wallet> active = walletDao.findAllByUserId(userId).stream().filter(w -> !w.isArchived()).toList();
        BigDecimal total = active.stream().map(Wallet::getCurrentBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        r.set("wallets", json.toTree(active.stream()
                .map(w -> Map.of("name", w.getName(), "balance", money(w.getCurrentBalance()))).toList()));
        r.put("totalBalance", money(total));
    }

    private void transactions(Long userId, Plan p, ObjectNode r) {
        r.put("from", p.from().toString());
        r.put("to", p.to().toString());
        if (p.category() != null) {
            r.put("category", p.category().getName());
        }
        if (p.wallet() != null) {
            r.put("wallet", p.wallet().getName());
        }
        TransactionFilter filter = filter(p, p.from(), p.to());

        switch (p.metric()) {
            case "list" -> {
                List<Transaction> items = transactionDao.search(userId, filter, 0, LIST_LIMIT);
                r.put("totalCount", transactionDao.countSearch(userId, filter));
                ArrayNode list = r.putArray("latestTransactions");
                for (Transaction t : items) {
                    ObjectNode row = list.addObject();
                    row.put("date", t.getTxnDate().toString());
                    row.put("type", t.getType().name());
                    row.put("amount", money(t.getAmount()));
                    row.put("category", t.getCategory() == null ? null : t.getCategory().getName());
                    row.put("wallet", t.getWallet().getName());
                    row.put("note", t.getNote());
                }
            }
            case "count" -> r.put("count", transactionDao.countSearch(userId, filter));
            default -> {
                if ("category".equals(p.groupBy())) {
                    TransactionType type = p.type() == null ? TransactionType.EXPENSE : p.type();
                    r.put("type", type.name());
                    r.set("byCategory", json.toTree(transactionDao.sumByCategory(userId, type, p.from(), p.to()).stream()
                            .map(c -> Map.of("category", c.categoryName(), "amount", money(c.amount()))).toList()));
                } else if ("month".equals(p.groupBy())) {
                    r.set("byMonth", json.toTree(transactionDao.monthlyTotals(userId, p.from(), p.to()).stream()
                            .map(m -> Map.of("month", "%d-%02d".formatted(m.year(), m.month()),
                                    "income", money(m.income()), "expense", money(m.expense()))).toList()));
                } else if ("wallet".equals(p.groupBy())) {
                    Map<String, BigDecimal> byWallet = new LinkedHashMap<>();
                    TransactionType type = p.type() == null ? TransactionType.EXPENSE : p.type();
                    for (Transaction t : transactionDao.findAll(userId, filter)) {
                        if (t.getType() == type) {
                            byWallet.merge(t.getWallet().getName(), t.getAmount(), BigDecimal::add);
                        }
                    }
                    r.put("type", type.name());
                    r.set("byWallet", json.toTree(byWallet.entrySet().stream()
                            .map(e -> Map.of("wallet", e.getKey(), "amount", money(e.getValue()))).toList()));
                } else {
                    sums(userId, p, filter, r, "");
                    if (p.comparePrevious()) {
                        // AF-2: so với kỳ liền trước cùng độ dài
                        long days = ChronoUnit.DAYS.between(p.from(), p.to()) + 1;
                        LocalDate prevTo = p.from().minusDays(1);
                        LocalDate prevFrom = prevTo.minusDays(days - 1);
                        ObjectNode prev = r.putObject("previousPeriod");
                        prev.put("from", prevFrom.toString());
                        prev.put("to", prevTo.toString());
                        sums(userId, p, filter(p, prevFrom, prevTo), prev, "");
                        compare(userId, p, filter, filter(p, prevFrom, prevTo), r);
                    }
                }
            }
        }
    }

    private void sums(Long userId, Plan p, TransactionFilter filter, ObjectNode r, String prefix) {
        var totals = transactionDao.totals(userId, filter);
        if (p.type() != TransactionType.INCOME) {
            r.put(prefix + "totalExpense", money(totals.expense()));
        }
        if (p.type() != TransactionType.EXPENSE) {
            r.put(prefix + "totalIncome", money(totals.income()));
        }
        if ("avg".equals(p.metric())) {
            long count = transactionDao.countSearch(userId, filter);
            BigDecimal sum = p.type() == TransactionType.INCOME ? totals.income() : totals.expense();
            r.put(prefix + "transactionCount", count);
            r.put(prefix + "averagePerTransaction", count == 0 ? money(BigDecimal.ZERO)
                    : money(sum.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP)));
        }
    }

    private void compare(Long userId, Plan p, TransactionFilter current, TransactionFilter previous, ObjectNode r) {
        boolean income = p.type() == TransactionType.INCOME;
        var now = transactionDao.totals(userId, current);
        var before = transactionDao.totals(userId, previous);
        BigDecimal a = income ? now.income() : now.expense();
        BigDecimal b = income ? before.income() : before.expense();
        BigDecimal diff = a.subtract(b);
        r.put("differenceVsPrevious", (diff.signum() >= 0 ? "+" : "-") + money(diff.abs()));
        if (b.signum() != 0) {
            BigDecimal pct = diff.multiply(BigDecimal.valueOf(100)).divide(b, 1, RoundingMode.HALF_UP);
            r.put("percentChangeVsPrevious", (pct.signum() >= 0 ? "+" : "") + pct.toPlainString() + "%");
        }
    }

    private static TransactionFilter filter(Plan p, LocalDate from, LocalDate to) {
        return new TransactionFilter(from, to, p.wallet() == null ? null : p.wallet().getId(),
                p.category() == null ? null : p.category().getId(), p.type(), null, null, null, null);
    }

    /** Tên trùng giữa danh mục thu và chi (ví dụ "Khác") thì ưu tiên danh mục cùng loại giao dịch. */
    private static Category byName(String name, TransactionType type, List<Category> categories) {
        if (name == null) {
            return null;
        }
        List<Category> matches = categories.stream().filter(c -> c.getName().equalsIgnoreCase(name.trim())).toList();
        return matches.stream()
                .filter(c -> type == null || c.getType().name().equals(type.name()))
                .findFirst()
                .orElse(matches.isEmpty() ? null : matches.get(0));
    }

    private static LocalDate date(String text) {
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String money(BigDecimal amount) {
        return MoneyFormatter.format(amount);
    }
}
