package com.fintech.chatbot.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.chatbot.ai.AiSettings;
import com.fintech.chatbot.ai.AiTask;
import com.fintech.chatbot.ai.Intent;
import com.fintech.chatbot.ai.JsonSupport;
import com.fintech.chatbot.ai.Prompts;
import com.fintech.chatbot.ai.client.ChatMessage;
import com.fintech.chatbot.ai.client.LlmClient;
import com.fintech.chatbot.common.util.MoneyFormatter;
import com.fintech.chatbot.common.util.MoneyParser;
import com.fintech.chatbot.dao.CategoryDao;
import com.fintech.chatbot.dao.WalletDao;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.Wallet;
import com.fintech.chatbot.entity.enums.CategoryType;
import com.fintech.chatbot.entity.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * UC-A11 bước 3–5 — Trích xuất giao dịch từ câu nói tự nhiên thành các thẻ nháp để user xác nhận (BR-04).
 *
 * <p>LLM chỉ đề xuất; code chuẩn hóa lại: số tiền theo BR-08 (MoneyParser), ngày phải hợp lệ và không ở tương lai,
 * danh mục / ví phải khớp dữ liệu thật của user (không khớp → "Khác" / ví mặc định).
 */
@Component
@RequiredArgsConstructor
public class TransactionExtractor {

    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    private final LlmClient llm;
    private final JsonSupport json;
    private final CategoryDao categoryDao;
    private final WalletDao walletDao;
    private final Clock clock;

    /** Thẻ giao dịch nháp; client gửi lại (có thể đã sửa) để lưu. */
    public record Draft(TransactionType type, BigDecimal amount, Long categoryId, String categoryName,
                        Long walletId, String walletName, LocalDate txnDate, String note) {
    }

    public HandlerResult handle(Long userId, String message, AiSettings.Snapshot settings) {
        LocalDate today = LocalDate.now(clock);
        List<Category> expense = categoryDao.findVisibleForUser(userId, CategoryType.EXPENSE);
        List<Category> income = categoryDao.findVisibleForUser(userId, CategoryType.INCOME);
        List<Wallet> wallets = walletDao.findAllByUserId(userId).stream().filter(w -> !w.isArchived()).toList();

        String system = Prompts.EXTRACT_TRANSACTIONS.formatted(today,
                today.getDayOfWeek().getDisplayName(TextStyle.FULL, VI), names(expense), names(income),
                String.join(", ", wallets.stream().map(Wallet::getName).toList()));
        String raw = llm.chat(settings.model(AiTask.EXTRACTION), List.of(ChatMessage.system(system), ChatMessage.user(message)), 0);
        JsonNode items = json.extractObject(raw).map(n -> n.path("transactions")).orElse(null);

        if (items == null || !items.isArray() || items.isEmpty()) {
            return HandlerResult.text(Intent.RECORD_TRANSACTION,
                    "Mình chưa hiểu, bạn có thể ghi lại theo dạng: [nội dung] [số tiền], ví dụ \"ăn trưa 45k\" nhé?");
        }

        Wallet defaultWallet = walletDao.findDefaultByUserId(userId)
                .orElse(wallets.isEmpty() ? null : wallets.get(0));
        List<Draft> drafts = new ArrayList<>();
        for (JsonNode item : items) {
            Optional<BigDecimal> amount = amountOf(item);
            if (amount.isEmpty()) {
                // AF-1: thiếu số tiền → hỏi lại, chưa tạo thẻ nào
                String what = JsonSupport.text(item, "note");
                return HandlerResult.text(Intent.RECORD_TRANSACTION,
                        what == null ? "Khoản này bao nhiêu tiền vậy bạn?" : "Khoản \"%s\" bao nhiêu tiền vậy bạn?".formatted(what));
            }
            TransactionType type = "INCOME".equalsIgnoreCase(JsonSupport.text(item, "type"))
                    ? TransactionType.INCOME : TransactionType.EXPENSE;
            Category category = matchCategory(JsonSupport.text(item, "category"),
                    type == TransactionType.INCOME ? income : expense);
            Wallet wallet = matchWallet(JsonSupport.text(item, "wallet"), wallets).orElse(defaultWallet);
            drafts.add(new Draft(type, amount.get(),
                    category == null ? null : category.getId(), category == null ? null : category.getName(),
                    wallet == null ? null : wallet.getId(), wallet == null ? null : wallet.getName(),
                    dateOf(item, today), JsonSupport.text(item, "note")));
        }

        String content = drafts.size() == 1
                ? "Mình ghi nhận khoản sau, bạn kiểm tra rồi bấm Lưu nhé:"
                : "Mình ghi nhận %d khoản sau, bạn kiểm tra rồi bấm \"Lưu tất cả\" nhé:".formatted(drafts.size());
        StringBuilder sb = new StringBuilder(content);
        for (Draft d : drafts) {
            sb.append("\n- ").append(d.type() == TransactionType.INCOME ? "Thu " : "Chi ")
                    .append(MoneyFormatter.format(d.amount()))
                    .append(d.categoryName() == null ? "" : " · " + d.categoryName())
                    .append(d.note() == null ? "" : " · " + d.note());
        }
        var payload = json.object();
        payload.put("type", "TRANSACTION_DRAFTS");
        payload.set("drafts", json.toTree(drafts));
        return HandlerResult.withPayload(Intent.RECORD_TRANSACTION, sb.toString(), payload);
    }

    /**
     * UC-A14 — Quét hóa đơn: LLM đọc ảnh, code chuẩn hóa như UC-A11 rồi đối chiếu tiền (BR-06):
     * giảm giá của cả hóa đơn được chia theo tỷ lệ vào từng món để tổng các thẻ đúng bằng số tiền đã trả;
     * tổng các món thấp hơn số tiền thanh toán thì cảnh báo (có thể sót dòng). Hóa đơn dài được gộp theo danh mục.
     */
    public HandlerResult handleReceipt(Long userId, byte[] image, String mimeType, AiSettings.Snapshot settings) {
        LocalDate today = LocalDate.now(clock);
        List<Category> expense = categoryDao.findVisibleForUser(userId, CategoryType.EXPENSE);
        List<Wallet> wallets = walletDao.findAllByUserId(userId).stream().filter(w -> !w.isArchived()).toList();

        String system = Prompts.EXTRACT_RECEIPT.formatted(today, names(expense),
                String.join(", ", wallets.stream().map(Wallet::getName).toList()));
        String raw = llm.vision(settings.model(AiTask.EXTRACTION), system, "Ảnh hóa đơn của người dùng.", image, mimeType, 0);
        JsonNode root = json.extractObject(raw).orElse(null);

        List<ReceiptLine> lines = new ArrayList<>();
        if (root != null && root.path("transactions").isArray()) {
            for (JsonNode item : root.path("transactions")) {
                amountOf(item).ifPresent(amount -> lines.add(new ReceiptLine(amount,
                        matchCategory(JsonSupport.text(item, "category"), expense), JsonSupport.text(item, "note"))));
            }
        }
        if (lines.isEmpty()) {
            return HandlerResult.text(Intent.RECORD_TRANSACTION,
                    "Mình không đọc được hóa đơn trong ảnh. Bạn chụp lại thẳng, đủ sáng và rõ các dòng giá tiền nhé.");
        }

        String merchant = JsonSupport.text(root, "merchant");
        LocalDate date = dateOf(root, today);
        Wallet wallet = matchWallet(JsonSupport.text(root, "wallet"), wallets)
                .orElse(walletDao.findDefaultByUserId(userId).orElse(wallets.isEmpty() ? null : wallets.get(0)));

        BigDecimal itemsTotal = lines.stream().map(ReceiptLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = positive(root, "discount").orElse(BigDecimal.ZERO);
        // Số tiền thật đã trả: ưu tiên dòng tổng thanh toán đọc được trên hóa đơn
        BigDecimal paid = positive(root, "total").orElse(itemsTotal.subtract(discount).max(BigDecimal.ONE));
        String warning = null;
        List<ReceiptLine> adjusted = lines;
        if (paid.compareTo(itemsTotal) < 0) {
            adjusted = allocate(lines, paid, itemsTotal);
        } else if (paid.compareTo(itemsTotal) > 0) {
            warning = "Tổng các món (%s) thấp hơn số tiền thanh toán (%s), có thể còn dòng mình chưa đọc được. Bạn kiểm tra lại nhé."
                    .formatted(MoneyFormatter.format(itemsTotal), MoneyFormatter.format(paid));
        }
        if (adjusted.size() > MAX_RECEIPT_DRAFTS) {
            adjusted = groupByCategory(adjusted);
        }

        List<Draft> drafts = new ArrayList<>();
        for (ReceiptLine line : adjusted) {
            String note = line.note() == null ? merchant : line.note();
            drafts.add(new Draft(TransactionType.EXPENSE, line.amount(),
                    line.category() == null ? null : line.category().getId(),
                    line.category() == null ? null : line.category().getName(),
                    wallet == null ? null : wallet.getId(), wallet == null ? null : wallet.getName(), date, note));
        }

        StringBuilder sb = new StringBuilder("Mình đọc được hóa đơn")
                .append(merchant == null ? "" : " " + merchant)
                .append(" ngày ").append(date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append(", thanh toán ").append(MoneyFormatter.format(paid)).append('.');
        if (paid.compareTo(itemsTotal) < 0) {
            sb.append(" Khoản giảm ").append(MoneyFormatter.format(itemsTotal.subtract(paid)))
                    .append(" đã được chia theo tỷ lệ vào từng món.");
        }
        if (warning != null) {
            sb.append('\n').append(warning);
        }
        sb.append(drafts.size() == 1 ? "\nBạn kiểm tra rồi bấm Lưu nhé:" : "\nBạn kiểm tra rồi bấm \"Lưu tất cả\" nhé:");
        for (Draft d : drafts) {
            sb.append("\n- Chi ").append(MoneyFormatter.format(d.amount()))
                    .append(d.categoryName() == null ? "" : " · " + d.categoryName())
                    .append(d.note() == null ? "" : " · " + d.note());
        }
        var payload = json.object();
        payload.put("type", "TRANSACTION_DRAFTS");
        payload.set("drafts", json.toTree(drafts));
        return HandlerResult.withPayload(Intent.RECORD_TRANSACTION, sb.toString(), payload);
    }

    /** Hóa đơn dài hơn số này thì gộp các món cùng danh mục thành một thẻ. */
    static final int MAX_RECEIPT_DRAFTS = 8;

    record ReceiptLine(BigDecimal amount, Category category, String note) {
    }

    /**
     * Chia {@code paid} theo tỷ lệ giá từng món. Tổng thanh toán chẵn nghìn thì làm tròn mỗi món tới nghìn đồng
     * (dễ đọc: 61.000đ thay vì 61.306đ), không thì tới đồng; phần chênh còn lại cộng vào món lớn nhất để tổng khớp.
     */
    static List<ReceiptLine> allocate(List<ReceiptLine> lines, BigDecimal paid, BigDecimal itemsTotal) {
        BigDecimal unit = paid.remainder(BigDecimal.valueOf(1000)).signum() == 0 ? BigDecimal.valueOf(1000) : BigDecimal.ONE;
        List<ReceiptLine> result = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO;
        int largest = 0;
        for (int i = 0; i < lines.size(); i++) {
            ReceiptLine l = lines.get(i);
            BigDecimal share = l.amount().multiply(paid).divide(itemsTotal.multiply(unit), 0, java.math.RoundingMode.HALF_UP)
                    .multiply(unit);
            result.add(new ReceiptLine(share, l.category(), l.note()));
            assigned = assigned.add(share);
            if (l.amount().compareTo(lines.get(largest).amount()) > 0) {
                largest = i;
            }
        }
        ReceiptLine big = result.get(largest);
        result.set(largest, new ReceiptLine(big.amount().add(paid.subtract(assigned)), big.category(), big.note()));
        result.removeIf(l -> l.amount().signum() <= 0);
        return result;
    }

    private static List<ReceiptLine> groupByCategory(List<ReceiptLine> lines) {
        java.util.Map<Category, List<ReceiptLine>> groups = new java.util.LinkedHashMap<>();
        for (ReceiptLine l : lines) {
            groups.computeIfAbsent(l.category(), k -> new ArrayList<>()).add(l);
        }
        List<ReceiptLine> result = new ArrayList<>();
        groups.forEach((category, items) -> {
            BigDecimal sum = items.stream().map(ReceiptLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<String> notes = items.stream().map(ReceiptLine::note).filter(java.util.Objects::nonNull).toList();
            String note = notes.size() <= 3 ? String.join(", ", notes)
                    : String.join(", ", notes.subList(0, 3)) + " và %d món khác".formatted(notes.size() - 3);
            result.add(new ReceiptLine(sum, category, note.isEmpty() ? null : note));
        });
        return result;
    }

    private static Optional<BigDecimal> positive(JsonNode root, String field) {
        JsonNode n = root.path(field);
        if (n.isNumber()) {
            BigDecimal v = n.decimalValue().setScale(0, java.math.RoundingMode.HALF_UP);
            return v.signum() > 0 ? Optional.of(v) : Optional.empty();
        }
        return MoneyParser.parse(JsonSupport.text(root, field)).filter(v -> v.signum() > 0);
    }

    /** Ưu tiên cụm chữ người dùng nói (BR-08), sau đó mới tới số LLM tự quy đổi. */
    private static Optional<BigDecimal> amountOf(JsonNode item) {
        Optional<BigDecimal> fromText = MoneyParser.parse(JsonSupport.text(item, "amount_text"));
        if (fromText.isPresent()) {
            return fromText;
        }
        JsonNode n = item.path("amount");
        if (n.isNumber() && n.decimalValue().signum() > 0) {
            return Optional.of(n.decimalValue().setScale(0, java.math.RoundingMode.HALF_UP));
        }
        return MoneyParser.parse(JsonSupport.text(item, "amount"));
    }

    private static LocalDate dateOf(JsonNode item, LocalDate today) {
        String text = JsonSupport.text(item, "date");
        if (text == null) {
            return today;
        }
        try {
            LocalDate d = LocalDate.parse(text);
            return d.isAfter(today) || d.isBefore(today.minusYears(1)) ? today : d;
        } catch (DateTimeParseException e) {
            return today;
        }
    }

    private static Category matchCategory(String name, List<Category> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        String wanted = name == null ? "khác" : name.trim().toLowerCase(VI);
        return candidates.stream().filter(c -> c.getName().toLowerCase(VI).equals(wanted)).findFirst()
                .or(() -> candidates.stream().filter(c -> c.getName().toLowerCase(VI).contains(wanted)
                        || wanted.contains(c.getName().toLowerCase(VI))).findFirst())
                .or(() -> candidates.stream().filter(c -> c.getName().equalsIgnoreCase("Khác")).findFirst())
                .orElse(candidates.get(0));
    }

    private static Optional<Wallet> matchWallet(String name, List<Wallet> wallets) {
        if (name == null) {
            return Optional.empty();
        }
        String wanted = name.toLowerCase(VI);
        return wallets.stream().filter(w -> w.getName().toLowerCase(VI).equals(wanted)).findFirst()
                .or(() -> wallets.stream().filter(w -> w.getName().toLowerCase(VI).contains(wanted)
                        || wanted.contains(w.getName().toLowerCase(VI))).findFirst());
    }

    private static String names(List<Category> categories) {
        return String.join(", ", categories.stream().map(Category::getName).distinct().toList());
    }
}
