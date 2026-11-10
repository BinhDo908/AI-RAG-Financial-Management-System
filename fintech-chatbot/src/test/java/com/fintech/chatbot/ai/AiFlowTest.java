package com.fintech.chatbot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dto.request.AiDataPermissionRequest;
import com.fintech.chatbot.dto.request.BudgetRequest;
import com.fintech.chatbot.dto.request.ChatRequest;
import com.fintech.chatbot.dto.request.ConfirmTransactionsRequest;
import com.fintech.chatbot.dto.request.DocumentMetadataRequest;
import com.fintech.chatbot.dto.request.EvaluationRequest;
import com.fintech.chatbot.dto.request.FeedbackRequest;
import com.fintech.chatbot.dto.request.RegisterRequest;
import com.fintech.chatbot.dto.request.ReviewFeedbackRequest;
import com.fintech.chatbot.dto.request.TransactionRequest;
import com.fintech.chatbot.dto.response.ChatResponse;
import com.fintech.chatbot.entity.enums.CategoryType;
import com.fintech.chatbot.entity.enums.DocumentStatus;
import com.fintech.chatbot.entity.enums.FeedbackRating;
import com.fintech.chatbot.entity.enums.FeedbackReason;
import com.fintech.chatbot.entity.enums.FeedbackRootCause;
import com.fintech.chatbot.entity.enums.InputMode;
import com.fintech.chatbot.entity.enums.ReviewStatus;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.service.AiConfigService;
import com.fintech.chatbot.service.AiDataPermissionService;
import com.fintech.chatbot.service.AuthService;
import com.fintech.chatbot.service.BudgetService;
import com.fintech.chatbot.service.ChatQualityService;
import com.fintech.chatbot.service.ChatService;
import com.fintech.chatbot.service.ConversationService;
import com.fintech.chatbot.service.FeedbackService;
import com.fintech.chatbot.service.KnowledgeBaseService;
import com.fintech.chatbot.service.TransactionService;
import com.fintech.chatbot.service.UserCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử giai đoạn 3 (A11–A13, B01–B03, B07, B09, C01–C04) với AI giả lập (FakeAiConfig).
 */
@SpringBootTest(properties = "app.ai.chat.model=fake-model")
@ActiveProfiles("test")
@Import(FakeAiConfig.class)
class AiFlowTest {

    private static final String KNOWLEDGE_DOC = """
            # Quỹ khẩn cấp
            Quỹ khẩn cấp là khoản tiền dự phòng cho các tình huống bất ngờ như mất việc hay ốm đau.
            Quỹ khẩn cấp nên bằng 3 đến 6 tháng chi tiêu thiết yếu và để ở nơi dễ rút.

            # Thẻ tín dụng
            Thẻ tín dụng tính lãi trên dư nợ nếu không thanh toán đủ trước ngày đến hạn.
            """;

    @Autowired AuthService authService;
    @Autowired ChatService chatService;
    @Autowired TransactionService transactionService;
    @Autowired UserCategoryService categoryService;
    @Autowired BudgetService budgetService;
    @Autowired AiDataPermissionService permissionService;
    @Autowired KnowledgeBaseService knowledgeService;
    @Autowired FeedbackService feedbackService;
    @Autowired ChatQualityService qualityService;
    @Autowired AiConfigService aiConfigService;
    @Autowired ConversationService conversationService;
    @Autowired FakeAiConfig.FakeRates fakeRates;

    Long userId;
    Long foodId;

    @BeforeEach
    void setUp() {
        // Kho tri thức dùng chung giữa các test: dọn sạch để kết quả truy xuất không phụ thuộc thứ tự test
        knowledgeService.list(null, null, 0, 100).items().forEach(d -> knowledgeService.delete(d.id()));
        userId = register();
        foodId = categoryService.list(userId, CategoryType.EXPENSE, false).stream()
                .filter(c -> c.name().equals("Ăn uống")).findFirst().orElseThrow().id();
    }

    // ---------- UC-A11, A12: ghi giao dịch bằng chat / giọng nói ----------

    @Test
    void chatExtractsDraftsThenUserConfirmsWithVoiceSource() {
        budgetService.create(userId, new BudgetRequest(foodId, YearMonth.now().toString(), money(1_000_000), null));

        ChatResponse r = send(null, "trưa nay ăn phở 50k, được thưởng tám mươi nghìn", InputMode.VOICE);
        assertThat(r.reply().intent()).isEqualTo("RECORD_TRANSACTION");
        assertThat(r.userMessage().inputMode()).isEqualTo(InputMode.VOICE);
        assertThat(r.conversationTitle()).startsWith("trưa nay ăn phở 50k");

        JsonNode drafts = r.reply().payload().path("drafts");
        assertThat(r.reply().payload().path("type").asText()).isEqualTo("TRANSACTION_DRAFTS");
        assertThat(drafts).hasSize(2);
        assertThat(drafts.get(0).path("amount").asLong()).isEqualTo(50_000);
        assertThat(drafts.get(0).path("categoryName").asText()).isEqualTo("Ăn uống");
        assertThat(drafts.get(0).path("walletName").asText()).isEqualTo("Tiền mặt");
        assertThat(drafts.get(1).path("type").asText()).isEqualTo("INCOME");
        assertThat(drafts.get(1).path("amount").asLong()).isEqualTo(80_000);
        // Ngày tương lai do LLM trả về bị thay bằng hôm nay
        assertThat(drafts.get(1).path("txnDate").asText()).isEqualTo(LocalDate.now().toString());

        // BR-04: chưa lưu gì cho tới khi user xác nhận
        assertThat(transactionService.search(userId, filterAll(), 0, 10).page().totalItems()).isZero();

        var confirmed = chatService.confirmTransactions(userId, new ConfirmTransactionsRequest(r.conversationId(),
                InputMode.VOICE, List.of(fromDraft(drafts.get(0)), fromDraft(drafts.get(1)))));
        assertThat(confirmed.results()).hasSize(2)
                .allSatisfy(t -> assertThat(t.transaction().source()).isEqualTo(TransactionSource.VOICE));
        assertThat(confirmed.reply().content()).contains("Đã lưu 2 giao dịch", "Ngân sách Ăn uống", "950.000đ");

        var history = conversationService.get(userId, r.conversationId());
        assertThat(history.messages()).hasSize(3);
        assertThat(history.messages().get(2).payload().path("type").asText()).isEqualTo("TRANSACTIONS_SAVED");
    }

    @Test
    void missingAmountAsksBackAndUnclearMessageAsksToRephrase() {
        assertThat(send(null, "mua cà phê", InputMode.TEXT).reply().content()).contains("Cà phê", "bao nhiêu tiền");
        assertThat(send(null, "linh tinh", InputMode.TEXT).reply().content()).contains("chưa hiểu");
    }

    // ---------- UC-A14: quét hóa đơn ----------

    @Test
    void receiptBecomesDraftsWithDiscountAllocatedThenSavedAsReceiptSource() {
        ChatResponse r = chatService.receipt(userId, null, new byte[2048], "image/jpeg");

        assertThat(r.userMessage().inputMode()).isEqualTo(InputMode.IMAGE);
        assertThat(r.userMessage().content()).isEqualTo("Ảnh hóa đơn");
        assertThat(r.reply().content()).contains("Siêu thị mini An Khang", "130.000đ", "chia theo tỷ lệ");
        JsonNode drafts = r.reply().payload().path("drafts");
        assertThat(drafts).hasSize(3);
        // BR-06: giảm giá chia theo tỷ lệ, tổng các thẻ đúng bằng số tiền đã trả
        long sum = 0;
        for (JsonNode d : drafts) {
            sum += d.path("amount").asLong();
            assertThat(d.path("walletName").asText()).isEqualTo("Tiền mặt");
            assertThat(d.path("txnDate").asText()).isEqualTo(LocalDate.now().minusDays(2).toString());
        }
        assertThat(sum).isEqualTo(130_000);
        // Tổng thanh toán chẵn nghìn → mỗi món làm tròn tới nghìn đồng
        assertThat(drafts.get(0).path("amount").asLong()).isEqualTo(63_000);
        assertThat(drafts.get(1).path("amount").asLong()).isEqualTo(25_000);
        assertThat(drafts.get(2).path("amount").asLong()).isEqualTo(42_000);
        assertThat(drafts.get(2).path("categoryName").asText()).isEqualTo("Mua sắm");

        var confirmed = chatService.confirmTransactions(userId, new ConfirmTransactionsRequest(r.conversationId(),
                InputMode.IMAGE, List.of(fromDraft(drafts.get(0)), fromDraft(drafts.get(1)), fromDraft(drafts.get(2)))));
        assertThat(confirmed.results()).allSatisfy(t -> assertThat(t.transaction().source()).isEqualTo(TransactionSource.RECEIPT));
    }

    @Test
    void receiptRejectsWrongFilesAndExplainsUnreadableImages() {
        assertError(ErrorCode.IMAGE_INVALID, () -> chatService.receipt(userId, null, new byte[100], "application/pdf"));
        assertError(ErrorCode.IMAGE_INVALID, () -> chatService.receipt(userId, null, new byte[9 * 1024 * 1024], "image/png"));
        assertThat(chatService.receipt(userId, null, new byte[1], "image/png").reply().content()).contains("không đọc được hóa đơn");
    }

    @Test
    void serverTranscriptionTrimsTextAndRejectsHugeAudio() {
        assertThat(chatService.transcribe(new byte[100], "a.webm").text()).isEqualTo("ăn sáng 30k");
        assertError(ErrorCode.AUDIO_INVALID, () -> chatService.transcribe(new byte[6 * 1024 * 1024], "a.webm"));
    }

    // ---------- UC-A13 ----------

    @Test
    void personalQueryNumbersComeFromCodeAndRespectPermissions() {
        Long cashId = cashWallet();
        transactionService.create(userId, new TransactionRequest(TransactionType.EXPENSE, money(120_000), cashId,
                null, foodId, LocalDate.now(), "Ăn tối"));

        ChatResponse r = send(null, "Tháng này tôi tiêu bao nhiêu cho ăn uống?", InputMode.TEXT);
        assertThat(r.reply().intent()).isEqualTo("QUERY_PERSONAL_DATA");
        // Fake LLM chỉ lặp lại dữ liệu → con số trong câu trả lời là do code tính
        assertThat(r.reply().content()).contains("120.000đ", "30 ngày gần nhất");
        assertThat(r.reply().payload().path("result").path("totalExpense").asText()).isEqualTo("120.000đ");

        permissionService.update(userId, new AiDataPermissionRequest(false, true, true, true, true));
        assertThat(send(null, "Ví còn bao nhiêu tiền?", InputMode.TEXT).reply().content())
                .contains("tắt quyền", "giao dịch");
    }

    @Test
    void streamingReportsStagesAndDeltasThenFullAnswer() {
        uploadApprovedDoc();
        List<String> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        StringBuilder streamed = new StringBuilder();
        ReplySink sink = new ReplySink() {
            @Override
            public void stage(Stage stage) {
                events.add(stage.name());
            }

            @Override
            public void delta(String text) {
                events.add("delta");
                streamed.append(text);
            }
        };

        ChatResponse r = chatService.send(userId, new ChatRequest(null, "Quỹ khẩn cấp nên bao nhiêu?", InputMode.TEXT), sink);

        assertThat(events).containsSubsequence("THINKING", "SEARCHING", "WRITING", "delta");
        // Câu trả lời cuối = phần đã stream + disclaimer; trích dẫn chỉ có ở bản cuối
        assertThat(r.reply().content()).startsWith(streamed.toString().trim()).contains("không phải lời khuyên đầu tư");
        assertThat(r.reply().citations()).isNotEmpty();

        // Câu rõ ràng: phân loại bằng luật, không stream chữ (thẻ giao dịch trả về ở bản cuối)
        events.clear();
        ChatResponse tx = chatService.send(userId, new ChatRequest(null, "trưa nay ăn phở 50k", InputMode.TEXT), sink);
        assertThat(tx.reply().intent()).isEqualTo("RECORD_TRANSACTION");
        assertThat(events).containsExactly("THINKING");
    }

    // ---------- UC-C01, B01 ----------

    @Test
    void knowledgeBaseLifecycleAndRagAnswerWithCitations() {
        Long docId = uploadApprovedDoc();

        ChatResponse r = send(null, "Quỹ khẩn cấp nên bao nhiêu?", InputMode.TEXT);
        assertThat(r.reply().intent()).isEqualTo("KNOWLEDGE_QA");
        assertThat(r.reply().noSource()).isFalse();
        assertThat(r.reply().content()).contains("[1]", "không phải lời khuyên đầu tư");
        assertThat(r.reply().citations()).isNotEmpty();
        assertThat(r.reply().citations().get(0).path("documentId").asLong()).isEqualTo(docId);
        assertThat(r.reply().citations().get(0).path("heading").asText()).isEqualTo("Quỹ khẩn cấp");

        // AF-1: không có nguồn phù hợp → không gọi LLM sinh câu trả lời, không bịa
        ChatResponse none = send(null, "Bảo hiểm nhân thọ là gì?", InputMode.TEXT);
        assertThat(none.reply().noSource()).isTrue();
        assertThat(none.reply().content()).contains("chưa tìm thấy thông tin này");

        // AF-3: thử truy vấn
        var test = knowledgeService.testQuery("quỹ khẩn cấp");
        assertThat(test.sufficient()).isTrue();
        assertThat(test.hits().get(0).usedInPrompt()).isTrue();

        // AF-1: tắt tài liệu → không còn được truy xuất
        knowledgeService.disable(docId);
        assertThat(send(null, "Quỹ khẩn cấp nên bao nhiêu?", InputMode.TEXT).reply().noSource()).isTrue();

        // AF-2: thay nội dung → về DRAFT, chia đoạn lại
        var replaced = knowledgeService.replaceContent(docId,
                "# Lãi suất\nLãi suất kép tính lãi trên cả gốc và lãi đã nhận.".getBytes(StandardCharsets.UTF_8), "v2.md");
        assertThat(replaced.document().status()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(replaced.document().embeddedCount()).isZero();
        assertThat(replaced.chunks()).singleElement().satisfies(c -> assertThat(c.heading()).isEqualTo("Lãi suất"));

        knowledgeService.delete(docId);
        assertError(ErrorCode.NOT_FOUND, () -> knowledgeService.get(docId));
    }

    @Test
    void knowledgeBaseRejectsDuplicatesAndUnsupportedFiles() {
        byte[] content = KNOWLEDGE_DOC.getBytes(StandardCharsets.UTF_8);
        knowledgeService.upload(adminId(), metadata("Bản gốc"), content, "goc.md");
        assertError(ErrorCode.DOCUMENT_DUPLICATE, () -> knowledgeService.upload(adminId(), metadata("Bản trùng"),
                content, "copy.md"));
        assertError(ErrorCode.DOCUMENT_UNSUPPORTED, () -> knowledgeService.upload(adminId(), metadata("Ảnh"),
                new byte[10], "anh.png"));
    }

    // ---------- UC-B02 ----------

    @Test
    void personalAdviceUsesCodeComputedSummary() {
        Long docId = uploadApprovedDoc();
        transactionService.create(userId, new TransactionRequest(TransactionType.EXPENSE, money(2_000_000), cashWallet(),
                null, foodId, LocalDate.now(), null));

        ChatResponse r = send(null, "Tôi nên tiết kiệm thế nào?", InputMode.TEXT);
        assertThat(r.reply().intent()).isEqualTo("PERSONAL_ADVICE");
        assertThat(r.reply().payload().path("summary").path("avgMonthlyExpense").asText()).isEqualTo("2.000.000đ");
        assertThat(r.reply().content()).contains("không phải lời khuyên đầu tư");
        knowledgeService.delete(docId);
    }

    @Test
    void personalAdviceWithoutDataFallsBackToGeneralKnowledge() {
        ChatResponse r = send(null, "Tôi nên tiết kiệm thế nào?", InputMode.TEXT);
        assertThat(r.reply().content()).contains("Hồ sơ tài chính");
    }

    // ---------- UC-B07 ----------

    @Test
    void marketRateIsComputedByCodeWithSourceAndCached() {
        int before = fakeRates.calls.get();
        ChatResponse r = send(null, "100 đô bằng bao nhiêu tiền Việt?", InputMode.TEXT);
        assertThat(r.reply().intent()).isEqualTo("MARKET_DATA");
        assertThat(r.reply().content()).contains("100 USD ≈ 2.500.000 VND", "Nguồn: nguồn giả lập");
        send(null, "100 đô bằng bao nhiêu tiền Việt?", InputMode.TEXT);
        assertThat(fakeRates.calls.get() - before).isLessThanOrEqualTo(1);
    }

    // ---------- Router: small talk, ngoài phạm vi, hỏi lại ----------

    @Test
    void routerHandlesSmallTalkOutOfScopeAndLowConfidence() {
        assertThat(send(null, "Chào bạn", InputMode.TEXT).reply().intent()).isEqualTo("SMALL_TALK");
        assertThat(send(null, "Kết quả bóng đá hôm qua", InputMode.TEXT).reply().content()).contains("chỉ hỗ trợ");
        ChatResponse clarify = send(null, "hmm", InputMode.TEXT);
        assertThat(clarify.reply().intent()).isEqualTo("CLARIFY");
        assertThat(clarify.reply().payload().path("type").asText()).isEqualTo("CLARIFY");
    }

    // ---------- UC-B09, C02, C03 ----------

    @Test
    void feedbackReportReviewAndDashboard() {
        Long docId = uploadApprovedDoc();
        ChatResponse r = send(null, "Quỹ khẩn cấp nên bao nhiêu?", InputMode.TEXT);
        Long answerId = r.reply().id();

        assertError(ErrorCode.FEEDBACK_NOT_ALLOWED, () -> feedbackService.rate(userId, r.userMessage().id(),
                new FeedbackRequest(FeedbackRating.UP, null, null)));
        Long other = register();
        assertError(ErrorCode.MESSAGE_NOT_FOUND, () -> feedbackService.rate(other, answerId,
                new FeedbackRequest(FeedbackRating.DOWN, FeedbackReason.WRONG_INFO, null)));

        var view = feedbackService.rate(userId, answerId,
                new FeedbackRequest(FeedbackRating.DOWN, FeedbackReason.WRONG_INFO, "Sai rồi"));
        assertThat(view.reviewStatus()).isEqualTo(ReviewStatus.OPEN);

        var detail = feedbackService.report(view.id());
        assertThat(detail.question()).isEqualTo("Quỹ khẩn cấp nên bao nhiêu?");
        assertThat(detail.retrieval().path("candidates")).isNotEmpty();
        assertThat(feedbackService.reports(ReviewStatus.OPEN, null, null, null, 0, 100).items())
                .anySatisfy(s -> assertThat(s.id()).isEqualTo(view.id()));

        var reviewed = feedbackService.review(view.id(),
                new ReviewFeedbackRequest(ReviewStatus.RESOLVED, FeedbackRootCause.MISSING_DOCUMENT, "Bổ sung tài liệu"));
        assertThat(reviewed.report().rootCause()).isEqualTo(FeedbackRootCause.MISSING_DOCUMENT);

        // Đổi ý thành 👍 → không còn trong hàng chờ
        feedbackService.rate(userId, answerId, new FeedbackRequest(FeedbackRating.UP, null, null));
        assertError(ErrorCode.NOT_FOUND, () -> feedbackService.report(view.id()));

        var dashboard = qualityService.dashboard(null, null);
        assertThat(dashboard.userMessages()).isPositive();
        assertThat(dashboard.intentDistribution()).containsKey("KNOWLEDGE_QA");
        assertThat(dashboard.thumbsUp()).isPositive();

        var eval = qualityService.evaluate(adminId(), new EvaluationRequest("bo-test", List.of(
                new EvaluationRequest.Item("Quỹ khẩn cấp nên bao nhiêu?", "3 đến 6 tháng chi tiêu", "SRC-05", null),
                new EvaluationRequest.Item("Bảo hiểm nhân thọ là gì?", "Hợp đồng bảo hiểm", "SRC-05", null))));
        assertThat(eval.run().totalQuestions()).isEqualTo(2);
        // Hit@k xét cả top_k (kể cả dưới ngưỡng); kho chỉ có 1 tài liệu SRC-05 nên cả 2 câu đều "trúng"
        assertThat(eval.run().hitRate()).isEqualTo(1.0);
        assertThat(eval.run().citationRate()).isEqualTo(0.5);
        assertThat(eval.run().accuracy()).isEqualTo(0.5);
        assertThat(qualityService.evaluations()).isNotEmpty();
        knowledgeService.delete(docId);
    }

    // ---------- UC-C04 ----------

    @Test
    void aiConfigValidationHistoryAndReset() {
        assertError(ErrorCode.AI_CONFIG_INVALID, () -> aiConfigService.update(adminId(), Map.of("rag.top_k", "25")));
        assertError(ErrorCode.AI_CONFIG_INVALID, () -> aiConfigService.update(adminId(), Map.of("rag.top_n", "9")));
        assertError(ErrorCode.AI_CONFIG_INVALID, () -> aiConfigService.update(adminId(), Map.of("khong.ton.tai", "1")));

        var updated = aiConfigService.update(adminId(), Map.of("llm.temperature", "0.7"));
        assertThat(updated.values()).containsEntry("llm.temperature", "0.7");
        assertThat(updated.effectiveModel()).isEqualTo("fake-model");
        assertThat(updated.history()).anySatisfy(h -> {
            assertThat(h.key()).isEqualTo("llm.temperature");
            assertThat(h.newValue()).isEqualTo("0.7");
        });

        var reset = aiConfigService.resetDefaults(adminId());
        assertThat(reset.values()).containsEntry("llm.temperature", "0.2");
    }

    // ---------- helpers ----------

    private Long uploadApprovedDoc() {
        String unique = KNOWLEDGE_DOC + "\nMã tài liệu " + UUID.randomUUID();
        var doc = knowledgeService.upload(adminId(), metadata("Cẩm nang tài chính cá nhân"),
                unique.getBytes(StandardCharsets.UTF_8), "cam-nang.md");
        assertThat(doc.document().status()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(doc.chunks()).hasSize(2);
        var approved = knowledgeService.approve(doc.document().id());
        assertThat(approved.status()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(approved.embeddedCount()).isEqualTo(2);
        return doc.document().id();
    }

    private static DocumentMetadataRequest metadata(String title) {
        return new DocumentMetadataRequest(title, "SRC-05", "https://www.consumerfinance.gov/", "Public domain", "vi", null);
    }

    private Long adminId() {
        return authService.login(new com.fintech.chatbot.dto.request.LoginRequest("admin@test.local", "AdminPass123"))
                .user().id();
    }

    private Long register() {
        String email = "ai-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        return authService.register(new RegisterRequest("AI Test", email, "Secret123", "Secret123", true)).user().id();
    }

    private ChatResponse send(Long conversationId, String text, InputMode mode) {
        return chatService.send(userId, new ChatRequest(conversationId, text, mode));
    }

    private Long cashWallet() {
        return transactionServiceWallet();
    }

    @Autowired com.fintech.chatbot.service.WalletService walletService;

    private Long transactionServiceWallet() {
        return walletService.list(userId, false).wallets().get(0).id();
    }

    private static TransactionRequest fromDraft(JsonNode d) {
        return new TransactionRequest(TransactionType.valueOf(d.path("type").asText()),
                d.path("amount").decimalValue(), d.path("walletId").asLong(), null, d.path("categoryId").asLong(),
                LocalDate.parse(d.path("txnDate").asText()), d.path("note").asText());
    }

    private static com.fintech.chatbot.dao.TransactionFilter filterAll() {
        return new com.fintech.chatbot.dao.TransactionFilter(null, null, null, null, null, null, null, null, null);
    }

    private static BigDecimal money(long v) {
        return BigDecimal.valueOf(v);
    }

    private static void assertError(ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", code);
    }
}
