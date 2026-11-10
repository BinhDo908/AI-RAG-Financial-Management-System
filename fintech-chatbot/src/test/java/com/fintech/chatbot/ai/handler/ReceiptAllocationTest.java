package com.fintech.chatbot.ai.handler;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC-A14 / BR-06 — Chia giảm giá của hóa đơn vào từng món: tổng luôn đúng bằng số tiền đã trả.
 */
class ReceiptAllocationTest {

    private static List<TransactionExtractor.ReceiptLine> lines(long... amounts) {
        return java.util.Arrays.stream(amounts)
                .mapToObj(a -> new TransactionExtractor.ReceiptLine(BigDecimal.valueOf(a), null, "món " + a))
                .toList();
    }

    private static long sum(List<TransactionExtractor.ReceiptLine> lines) {
        return lines.stream().mapToLong(l -> l.amount().longValueExact()).sum();
    }

    @Test
    void evenTotalIsRoundedToThousands() {
        // Hóa đơn mẫu: 6 món 344.500đ, giảm 14.500đ, thanh toán 330.000đ
        var result = TransactionExtractor.allocate(lines(64_000, 25_000, 38_000, 42_500, 79_000, 96_000),
                BigDecimal.valueOf(330_000), BigDecimal.valueOf(344_500));

        assertThat(sum(result)).isEqualTo(330_000);
        assertThat(result).allSatisfy(l -> assertThat(l.amount().longValueExact() % 1000).isZero());
        assertThat(result.get(0).amount()).isEqualByComparingTo("61000");
        assertThat(result.get(5).amount()).isEqualByComparingTo("92000");
    }

    @Test
    void oddTotalKeepsDongPrecision() {
        var result = TransactionExtractor.allocate(lines(50_000, 30_000, 20_000), BigDecimal.valueOf(99_999), BigDecimal.valueOf(100_000));
        assertThat(sum(result)).isEqualTo(99_999);
        assertThat(result.get(0).amount()).isEqualByComparingTo("49999"); // phần lẻ dồn vào món lớn nhất
    }
}
