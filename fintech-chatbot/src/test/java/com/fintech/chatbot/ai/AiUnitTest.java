package com.fintech.chatbot.ai;

import com.fintech.chatbot.ai.rag.TextChunker;
import com.fintech.chatbot.ai.rag.TextExtractor;
import com.fintech.chatbot.ai.rag.TextSection;
import com.fintech.chatbot.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử đơn vị các thành phần AI không cần Spring.
 */
class AiUnitTest {

    @Test
    void routerFallbackDetectsTransactionsWithoutLlm() {
        assertThat(IntentRouter.fallback("trưa nay ăn phở 50k").intent()).isEqualTo(Intent.RECORD_TRANSACTION);
        assertThat(IntentRouter.fallback("nhận lương 15 triệu").intent()).isEqualTo(Intent.RECORD_TRANSACTION);
        assertThat(IntentRouter.fallback("tháng này tiêu bao nhiêu?").intent()).isEqualTo(Intent.KNOWLEDGE_QA);
        assertThat(IntentRouter.fallback("thứ 2 đi học").intent()).isEqualTo(Intent.KNOWLEDGE_QA);
    }

    @Test
    void routerQuickPathOnlyForObviousMessages() {
        // Rõ ràng → không gọi LLM
        assertThat(IntentRouter.quick("trưa nay ăn phở 50k").intent()).isEqualTo(Intent.RECORD_TRANSACTION);
        assertThat(IntentRouter.quick("hôm qua đổ xăng 80 nghìn và mua sách 120k").intent()).isEqualTo(Intent.RECORD_TRANSACTION);
        assertThat(IntentRouter.quick("nhận lương 18tr5").intent()).isEqualTo(Intent.RECORD_TRANSACTION);
        assertThat(IntentRouter.quick("Cảm ơn bạn nhiều nhé!").intent()).isEqualTo(Intent.SMALL_TALK);
        assertThat(IntentRouter.quick("chào").intent()).isEqualTo(Intent.SMALL_TALK);
        // Có nghi ngờ → để LLM phân loại
        assertThat(IntentRouter.quick("100 đô bằng bao nhiêu tiền Việt?")).isNull();
        assertThat(IntentRouter.quick("tháng này tôi chi 5 triệu cho ăn uống")).isNull();
        assertThat(IntentRouter.quick("trả nợ anh Nam 2 triệu")).isNull();
        assertThat(IntentRouter.quick("lương 15 triệu thì nên để dành bao nhiêu")).isNull();
        assertThat(IntentRouter.quick("thứ 2 đi")).isNull();
        assertThat(IntentRouter.quick("chào bạn, quỹ khẩn cấp là gì")).isNull();
        assertThat(IntentRouter.quick("giá vàng 8 triệu một chỉ")).isNull();
    }

    @Test
    void chunkerRespectsSizeAndOverlapAndHeadings() {
        String paragraph = "Câu văn mẫu về quản lý tài chính cá nhân. ".repeat(30); // ~1300 ký tự
        String text = paragraph + "\n\n" + paragraph + "\n\n" + paragraph;
        List<TextSection> chunks = new TextChunker().chunk(List.of(
                new TextSection("Ngân sách", 3, text), new TextSection("Nợ", null, "Đoạn ngắn về nợ.")));

        assertThat(chunks).hasSizeGreaterThan(2);
        assertThat(chunks).allSatisfy(c -> assertThat(c.text().length()).isLessThanOrEqualTo(
                TextChunker.CHUNK_CHARS + TextChunker.OVERLAP_CHARS + 2));
        assertThat(chunks.get(0).heading()).isEqualTo("Ngân sách");
        assertThat(chunks.get(0).pageNumber()).isEqualTo(3);
        // Đoạn sau bắt đầu bằng phần cuối của đoạn trước (chồng lấn)
        String tail = chunks.get(0).text().substring(chunks.get(0).text().length() - 50);
        assertThat(chunks.get(1).text()).contains(tail.trim().substring(10));
        // Không vượt ranh giới đề mục
        assertThat(chunks.get(chunks.size() - 1)).satisfies(c -> {
            assertThat(c.heading()).isEqualTo("Nợ");
            assertThat(c.text()).isEqualTo("Đoạn ngắn về nợ.");
        });
    }

    @Test
    void extractorCleansTocLinesPageNumbersAndSplitsMarkdownHeadings() {
        String md = """
                # Mục lục
                Chương 1 .......... 3
                Trang 2
                # Ngân sách 50/30/20
                Dành 50% thu nhập cho nhu cầu thiết yếu,
                30% cho mong muốn và 20% để tiết kiệm.
                """;
        List<TextSection> sections = new TextExtractor().extract(md.getBytes(StandardCharsets.UTF_8), "a.md");
        assertThat(sections).singleElement().satisfies(s -> {
            assertThat(s.heading()).isEqualTo("Ngân sách 50/30/20");
            assertThat(s.text()).startsWith("Dành 50%").doesNotContain("......");
        });

        String html = "<html><head><title>T</title><script>x()</script></head><body><nav>menu</nav>"
                + "<h2>Thẻ tín dụng</h2><p>Thanh toán đủ dư nợ trước ngày đến hạn để không bị tính lãi.</p></body></html>";
        assertThat(new TextExtractor().extract(html.getBytes(StandardCharsets.UTF_8), "a.html"))
                .singleElement().satisfies(s -> {
                    assertThat(s.heading()).isEqualTo("Thẻ tín dụng");
                    assertThat(s.text()).doesNotContain("menu", "x()");
                });

        assertThatThrownBy(() -> new TextExtractor().extract("ngắn".getBytes(StandardCharsets.UTF_8), "a.txt"))
                .isInstanceOf(BusinessException.class);
    }
}
