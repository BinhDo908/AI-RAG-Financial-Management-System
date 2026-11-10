package com.fintech.chatbot.ai.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Nguồn tỷ giá (UC-B07). Cài đặt mặc định: open.er-api.com (SRC-12), miễn phí, không cần API key.
 */
public interface ExchangeRateProvider {

    /** Mã nguồn trong USE_CASES.md §4.2, ví dụ "SRC-12". */
    String sourceId();

    /** Tên hiển thị kèm câu trả lời (BR-12). */
    String sourceName();

    /** 1 {@code base} = {@code rate} {@code quote}. */
    Quote fetch(String base, String quote);

    record Quote(BigDecimal rate, LocalDateTime updatedAt) {
    }
}
