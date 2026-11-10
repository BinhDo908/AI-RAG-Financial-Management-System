package com.fintech.chatbot.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintech.chatbot.ai.AiSettings;
import com.fintech.chatbot.ai.AiTask;
import com.fintech.chatbot.ai.Intent;
import com.fintech.chatbot.ai.JsonSupport;
import com.fintech.chatbot.ai.Prompts;
import com.fintech.chatbot.ai.client.ChatMessage;
import com.fintech.chatbot.ai.client.LlmClient;
import com.fintech.chatbot.ai.market.MarketRateService;
import com.fintech.chatbot.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * UC-B07 — Tra cứu tỷ giá / quy đổi ngoại tệ. LLM chỉ trích mã tiền tệ và số tiền; tỷ giá lấy từ nguồn mở,
 * phép quy đổi do code tính và câu trả lời do code dựng (BR-06), luôn kèm nguồn và thời điểm (BR-12).
 * Giá vàng (SJC) và lãi suất (NHNN) chưa tích hợp vì chưa có endpoint công khai ổn định (USE_CASES.md §4.2).
 */
@Component
@RequiredArgsConstructor
public class MarketDataHandler {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final LlmClient llm;
    private final JsonSupport json;
    private final MarketRateService marketRateService;

    public HandlerResult handle(String question, AiSettings.Snapshot settings) {
        JsonNode q = json.extractObject(llm.chat(settings.model(AiTask.EXTRACTION),
                List.of(ChatMessage.system(Prompts.MARKET_QUERY), ChatMessage.user(question)), 0)).orElse(null);
        String kind = q == null ? "FX" : String.valueOf(JsonSupport.text(q, "kind")).toUpperCase(Locale.ROOT);
        if (!"FX".equals(kind)) {
            return HandlerResult.text(Intent.MARKET_DATA, "GOLD".equals(kind)
                    ? "Hiện mình chưa kết nối được nguồn giá vàng công khai (SJC). Bạn xem giá niêm yết trực tiếp tại sjc.com.vn nhé."
                    : "Hiện mình chưa kết nối được nguồn lãi suất. Bạn tham khảo lãi suất điều hành tại sbv.gov.vn "
                    + "hoặc biểu lãi suất công bố trên website của ngân hàng nhé.");
        }

        String base = currency(q == null ? null : JsonSupport.text(q, "base"), "USD");
        String quote = currency(q == null ? null : JsonSupport.text(q, "quote"), "VND");
        MarketRateService.FxRate rate;
        try {
            rate = marketRateService.fx(base, quote);
        } catch (BusinessException e) {
            return HandlerResult.text(Intent.MARKET_DATA, "Mình không lấy được tỷ giá %s/%s lúc này, bạn thử lại sau nhé."
                    .formatted(base, quote));
        }

        StringBuilder sb = new StringBuilder();
        BigDecimal amount = q == null || !q.path("amount").isNumber() ? null : q.path("amount").decimalValue();
        if (amount != null && amount.signum() > 0) {
            BigDecimal converted = amount.multiply(rate.rate());
            sb.append("%s %s ≈ %s %s".formatted(number(amount), base, number(converted), quote));
            sb.append(" (tỷ giá 1 %s = %s %s)".formatted(base, number(rate.rate()), quote));
        } else {
            sb.append("1 %s = %s %s".formatted(base, number(rate.rate()), quote));
        }
        sb.append("\nNguồn: ").append(rate.sourceName()).append(", cập nhật ").append(rate.fetchedAt().format(TIME));
        if (rate.stale()) {
            sb.append(" (nguồn đang lỗi, đây là dữ liệu đã lưu gần nhất)");
        }
        sb.append(".\nĐây là tỷ giá tham khảo, giá mua / bán thực tế tại ngân hàng có thể khác.");

        ObjectNode payload = json.object();
        payload.put("type", "FX_RATE");
        payload.put("base", base);
        payload.put("quote", quote);
        payload.put("rate", rate.rate());
        payload.put("sourceId", rate.sourceId());
        payload.put("fetchedAt", rate.fetchedAt().toString());
        return HandlerResult.withPayload(Intent.MARKET_DATA, sb.toString(), payload);
    }

    private static String currency(String code, String fallback) {
        return code != null && code.matches("[A-Za-z]{3}") ? code.toUpperCase(Locale.ROOT) : fallback;
    }

    /** Kiểu Việt Nam: 25.831,15 — tối đa 2 chữ số lẻ. */
    private static String number(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", symbols).format(value.setScale(2, RoundingMode.HALF_UP));
    }
}
