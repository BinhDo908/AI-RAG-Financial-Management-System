package com.fintech.chatbot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.chatbot.ai.client.ChatMessage;
import com.fintech.chatbot.ai.client.LlmClient;
import com.fintech.chatbot.common.util.MoneyParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * USE_CASES.md §9.1 — Điểm vào duy nhất của khung chat: phân loại ý định rồi chuyển tới use case tương ứng.
 * LLM trả {intent, confidence}; confidence thấp → hỏi lại. LLM trả sai định dạng → dùng luật đơn giản dự phòng.
 *
 * <p>Câu quá rõ ràng (chào hỏi / cảm ơn; kể khoản tiền mà không hỏi gì) được phân loại bằng luật, không gọi LLM:
 * tiết kiệm 1 lượt gọi (~1–6s) cho các tin nhắn phổ biến nhất. Có nghi ngờ thì luôn để LLM quyết định.
 */
@Component
@RequiredArgsConstructor
public class IntentRouter {

    static final double MIN_CONFIDENCE = 0.5;

    /** Cụm số tiền kiểu "50k", "2 triệu", "1tr2", "15.000.000đ". */
    private static final Pattern AMOUNT = Pattern.compile(
            "\\d+(?:[.,]\\d+)*\\s*(?:(?:k|nghìn|ngàn|tr|triệu|củ|tỷ|đ|vnd)(?!\\p{L}))?\\d*", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUESTION = Pattern.compile(
            "\\?|\\b(bao nhiêu|là gì|thế nào|như thế nào|có nên|nên|tại sao|vì sao|ra sao|không\\s*$)",
            Pattern.CASE_INSENSITIVE);
    /** Cả tin nhắn chỉ là lời chào / cảm ơn / tạm biệt. */
    private static final Pattern GREETING = Pattern.compile(
            "^(xin chào|chào|hello|hi|hey|alo|cảm ơn|cám ơn|thanks|thank you|thank|ok|oke|okay|tạm biệt|bye)"
                    + "(\\s+(bạn|bot|nhé|nha|ạ|nhiều|nhiều nhé|nhiều nha|bạn nhé|bạn nha|bạn nhiều|mình|em|anh|chị))*\\s*[!.]*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    /** Có số tiền nhưng có thể là ý định khác (tỷ giá, vay nợ, ngân sách, mục tiêu…) → để LLM phân loại. */
    private static final Pattern NOT_PLAIN_TRANSACTION = Pattern.compile(
            "(đô|usd|eur|euro|yên|tệ|ngoại tệ|tỷ giá|vàng|quy đổi|lãi|vay|nợ|ngân sách|hạn mức|mục tiêu|tiết kiệm|đầu tư|"
                    + "cổ phiếu|chứng khoán|coin|bảo hiểm|tháng này|tháng trước|tổng)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final int QUICK_MAX_LENGTH = 160;

    private final LlmClient llm;
    private final JsonSupport json;
    private final Clock clock;

    public record Decision(Intent intent, double confidence) {
    }

    public Decision route(String message, List<ChatMessage> history, AiSettings.Snapshot settings) {
        Decision quick = quick(message);
        if (quick != null) {
            return quick;
        }
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(Prompts.withHistory(Prompts.ROUTER.formatted(LocalDate.now(clock),
                String.join(", ", settings.blockedTopics())), history)));
        messages.add(ChatMessage.user(message));

        String raw = llm.chat(settings.model(AiTask.ROUTER), messages, 0);
        return json.extractObject(raw)
                .map(this::parse)
                .orElseGet(() -> fallback(message));
    }

    /** Tin nhắn có phải gọi LLM để phân loại không (không khớp luật nhanh). */
    public boolean needsLlm(String message) {
        return quick(message) == null;
    }

    private Decision parse(JsonNode node) {
        Intent intent;
        try {
            intent = Intent.valueOf(node.path("intent").asText("").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return new Decision(Intent.CLARIFY, 0);
        }
        double confidence = node.path("confidence").asDouble(1.0);
        if (confidence < MIN_CONFIDENCE && intent != Intent.SMALL_TALK) {
            return new Decision(Intent.CLARIFY, confidence);
        }
        return new Decision(intent, confidence);
    }

    /**
     * Phân loại bằng luật cho câu không cần LLM; {@code null} = để LLM quyết định.
     * Ghi giao dịch: có số tiền rõ ràng, không phải câu hỏi, không chứa từ khóa của ý định khác.
     */
    static Decision quick(String message) {
        String text = message.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty() || text.length() > QUICK_MAX_LENGTH) {
            return null;
        }
        if (GREETING.matcher(text).matches()) {
            return new Decision(Intent.SMALL_TALK, 1.0);
        }
        if (hasAmount(text) && !QUESTION.matcher(text).find() && !NOT_PLAIN_TRANSACTION.matcher(text).find()) {
            return new Decision(Intent.RECORD_TRANSACTION, 0.9);
        }
        return null;
    }

    /** Dự phòng khi LLM không trả JSON hợp lệ: có số tiền và không phải câu hỏi → ghi giao dịch. */
    static Decision fallback(String message) {
        String text = message.toLowerCase(Locale.ROOT);
        if (hasAmount(text) && !QUESTION.matcher(text).find()) {
            return new Decision(Intent.RECORD_TRANSACTION, 0.5);
        }
        return new Decision(Intent.KNOWLEDGE_QA, 0.5);
    }

    private static boolean hasAmount(String text) {
        Matcher m = AMOUNT.matcher(text);
        while (m.find()) {
            String candidate = m.group().trim();
            // Số trơn nhỏ ("thứ 2", "3 người") không phải số tiền: cần đơn vị tiền hoặc từ 4 chữ số
            boolean looksLikeMoney = candidate.matches(".*[^\\d.,].*") || candidate.replaceAll("\\D", "").length() >= 4;
            if (looksLikeMoney && MoneyParser.parse(candidate).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
