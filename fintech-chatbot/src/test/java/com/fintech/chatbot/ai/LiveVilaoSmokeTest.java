package com.fintech.chatbot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.chatbot.dto.request.ChatRequest;
import com.fintech.chatbot.dto.request.ConfirmTransactionsRequest;
import com.fintech.chatbot.dto.request.DocumentMetadataRequest;
import com.fintech.chatbot.dto.request.LoginRequest;
import com.fintech.chatbot.dto.request.RegisterRequest;
import com.fintech.chatbot.dto.request.TransactionRequest;
import com.fintech.chatbot.dto.response.ChatResponse;
import com.fintech.chatbot.entity.enums.InputMode;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.service.AuthService;
import com.fintech.chatbot.service.ChatService;
import com.fintech.chatbot.service.KnowledgeBaseService;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Chạy thử với API Vilao AI THẬT (tốn vài chục đồng). Mặc định bỏ qua; chỉ chạy khi đặt
 * {@code VILAO_LIVE=1} và {@code VILAO_API_KEY}. Dùng H2 thay PostgreSQL.
 */
@EnabledIfEnvironmentVariable(named = "VILAO_LIVE", matches = "1")
@SpringBootTest(properties = {
        "app.ai.chat.api-key=${VILAO_API_KEY}",
        "app.ai.embedding.api-key=${VILAO_API_KEY}"
})
@ActiveProfiles("test")
class LiveVilaoSmokeTest {

    private static final String DOC = """
            # Quỹ khẩn cấp
            Quỹ khẩn cấp là khoản tiền để dành riêng cho các tình huống bất ngờ như mất việc, ốm đau hay sửa chữa nhà cửa.
            Mức khuyến nghị phổ biến là từ 3 đến 6 tháng chi tiêu thiết yếu. Người có thu nhập không ổn định nên hướng tới 6 tháng.
            Quỹ khẩn cấp nên được giữ ở nơi an toàn và rút ra nhanh, ví dụ tài khoản tiết kiệm không kỳ hạn.

            # Quy tắc ngân sách 50/30/20
            Quy tắc 50/30/20 chia thu nhập sau thuế thành ba phần: 50% cho nhu cầu thiết yếu như nhà ở, ăn uống, đi lại;
            30% cho mong muốn như giải trí, mua sắm; 20% cho tiết kiệm và trả nợ.

            # Thẻ tín dụng
            Thẻ tín dụng cho phép chi tiêu trước, trả sau. Nếu thanh toán toàn bộ dư nợ trước ngày đến hạn thì không bị tính lãi;
            nếu chỉ trả tối thiểu, phần còn lại bị tính lãi suất cao trên toàn bộ dư nợ.
            """;

    @Autowired KnowledgeBaseService knowledge;
    @Autowired ChatService chat;
    @Autowired AuthService auth;

    private final List<String> report = new ArrayList<>();

    @Test
    void endToEndWithRealModels() {
        SoftAssertions soft = new SoftAssertions();
        knowledge.list(null, null, 0, 100).items().forEach(d -> knowledge.delete(d.id()));

        Long adminId = auth.login(new LoginRequest("admin@test.local", "AdminPass123")).user().id();
        var doc = knowledge.upload(adminId, new DocumentMetadataRequest("Cẩm nang tài chính cá nhân (tài liệu thử)",
                "SRC-05", null, "Tài liệu tự soạn để thử", "vi", null), DOC.getBytes(StandardCharsets.UTF_8), "cam-nang.md");
        long t0 = System.currentTimeMillis();
        var approved = knowledge.approve(doc.document().id());
        log("C01 approve: %d đoạn, embed %d đoạn, %d ms".formatted(approved.chunkCount(), approved.embeddedCount(),
                System.currentTimeMillis() - t0));
        soft.assertThat(approved.embeddedCount()).isEqualTo(approved.chunkCount());

        var probe = knowledge.testQuery("Quỹ khẩn cấp nên để bao nhiêu?");
        probe.hits().forEach(h -> log("  test-query score=%.3f used=%s heading=%s".formatted(h.score(), h.usedInPrompt(), h.heading())));

        String email = "live-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Long userId = auth.register(new RegisterRequest("Live Test", email, "Secret123", "Secret123", true)).user().id();

        ChatResponse rag = ask(userId, null, "Quỹ khẩn cấp nên để bao nhiêu tiền?");
        soft.assertThat(rag.reply().intent()).isIn("KNOWLEDGE_QA", "PERSONAL_ADVICE");
        soft.assertThat(rag.reply().noSource()).isFalse();
        soft.assertThat(rag.reply().citations()).isNotNull();

        ChatResponse none = ask(userId, null, "Bảo hiểm nhân thọ là gì?");
        soft.assertThat(none.reply().noSource()).isTrue();

        ChatResponse record = ask(userId, null, "trưa nay ăn phở 50k, tối uống trà sữa 35 nghìn");
        soft.assertThat(record.reply().intent()).isEqualTo("RECORD_TRANSACTION");
        JsonNode drafts = record.reply().payload() == null ? null : record.reply().payload().path("drafts");
        soft.assertThat(drafts).isNotNull();
        if (drafts != null && drafts.size() > 0) {
            List<TransactionRequest> toSave = new ArrayList<>();
            for (JsonNode d : drafts) {
                toSave.add(new TransactionRequest(TransactionType.valueOf(d.path("type").asText()), d.path("amount").decimalValue(),
                        d.path("walletId").asLong(), null, d.path("categoryId").asLong(),
                        LocalDate.parse(d.path("txnDate").asText()), d.path("note").asText()));
            }
            var saved = chat.confirmTransactions(userId, new ConfirmTransactionsRequest(record.conversationId(), InputMode.TEXT, toSave));
            log("A11 confirm: " + saved.reply().content().replace('\n', ' '));
        }

        ChatResponse query = ask(userId, null, "Hôm nay tôi đã chi bao nhiêu cho ăn uống?");
        soft.assertThat(query.reply().intent()).isEqualTo("QUERY_PERSONAL_DATA");
        soft.assertThat(query.reply().content()).contains("85.000");

        ChatResponse fx = ask(userId, null, "100 đô bằng bao nhiêu tiền Việt?");
        soft.assertThat(fx.reply().intent()).isEqualTo("MARKET_DATA");

        System.out.println("\n========== LIVE VILAO REPORT ==========");
        report.forEach(System.out::println);
        System.out.println("=======================================\n");
        knowledge.delete(doc.document().id());
        soft.assertAll();
    }

    private ChatResponse ask(Long userId, Long conversationId, String text) {
        ChatResponse r = chat.send(userId, new ChatRequest(conversationId, text, InputMode.TEXT));
        log("\nQ: %s\n   intent=%s noSource=%s latency=%dms citations=%s\n   A: %s".formatted(text, r.reply().intent(),
                r.reply().noSource(), r.reply().latencyMs(),
                r.reply().citations() == null ? 0 : r.reply().citations().size(),
                r.reply().content().replace("\n", "\n      ")));
        if (r.reply().payload() != null) {
            log("   payload=" + r.reply().payload());
        }
        return r;
    }

    private void log(String line) {
        report.add(line);
    }
}
