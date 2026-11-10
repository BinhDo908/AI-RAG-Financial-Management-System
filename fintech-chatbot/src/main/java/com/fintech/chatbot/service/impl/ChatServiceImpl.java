package com.fintech.chatbot.service.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintech.chatbot.ai.AiSettings;
import com.fintech.chatbot.ai.Intent;
import com.fintech.chatbot.ai.IntentRouter;
import com.fintech.chatbot.ai.JsonSupport;
import com.fintech.chatbot.ai.ReplySink;
import com.fintech.chatbot.ai.client.ChatMessage;
import com.fintech.chatbot.ai.client.LlmClient;
import com.fintech.chatbot.ai.client.SpeechToTextClient;
import com.fintech.chatbot.ai.handler.AdviceHandler;
import com.fintech.chatbot.ai.handler.HandlerResult;
import com.fintech.chatbot.ai.handler.KnowledgeAnswerer;
import com.fintech.chatbot.ai.handler.MarketDataHandler;
import com.fintech.chatbot.ai.handler.PersonalQueryHandler;
import com.fintech.chatbot.ai.handler.TransactionExtractor;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.util.MoneyFormatter;
import com.fintech.chatbot.common.util.Months;
import com.fintech.chatbot.dao.BudgetDao;
import com.fintech.chatbot.dao.ConversationDao;
import com.fintech.chatbot.dao.MessageDao;
import com.fintech.chatbot.dto.request.ChatRequest;
import com.fintech.chatbot.dto.request.ConfirmTransactionsRequest;
import com.fintech.chatbot.dto.request.TransactionRequest;
import com.fintech.chatbot.dto.response.ChatResponse;
import com.fintech.chatbot.dto.response.ConfirmTransactionsResponse;
import com.fintech.chatbot.dto.response.TransactionResult;
import com.fintech.chatbot.dto.response.TranscriptionResponse;
import com.fintech.chatbot.entity.Conversation;
import com.fintech.chatbot.entity.Message;
import com.fintech.chatbot.entity.enums.InputMode;
import com.fintech.chatbot.entity.enums.MessageRole;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.mapper.ChatMessageViews;
import com.fintech.chatbot.service.ChatService;
import com.fintech.chatbot.service.TransactionService;
import com.fintech.chatbot.service.support.BudgetCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    /** Số tin nhắn gần nhất đưa vào ngữ cảnh LLM. */
    private static final int HISTORY_MESSAGES = 6;
    private static final int TITLE_LENGTH = 60;
    private static final int MAX_AUDIO_BYTES = 5 * 1024 * 1024;
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    static final String RECEIPT_MESSAGE = "Ảnh hóa đơn";

    static final String SMALL_TALK_REPLY = """
            Chào bạn! Mình là trợ lý tài chính cá nhân. Bạn có thể:
            - Ghi thu chi bằng câu nói: "trưa nay ăn phở 50k"
            - Hỏi số liệu của mình: "tháng này tôi tiêu bao nhiêu cho ăn uống?"
            - Hỏi kiến thức tài chính: "quỹ khẩn cấp nên bao nhiêu?"
            - Xin lời khuyên: "tôi nên tiết kiệm thế nào?"
            - Tra tỷ giá: "100 đô bằng bao nhiêu tiền Việt?\"""";
    static final String OUT_OF_SCOPE_REPLY = "Xin lỗi, mình chỉ hỗ trợ các chủ đề tài chính cá nhân. Mình cũng không đưa ra "
            + "khuyến nghị mua / bán một cổ phiếu, coin hay sản phẩm cụ thể. Bạn có thể hỏi mình kiến thức chung, "
            + "ví dụ cách đa dạng hóa hay quản lý rủi ro khi đầu tư nhé.";
    static final String CLARIFY_REPLY = "Mình chưa chắc ý bạn: bạn muốn GHI một khoản thu / chi, hay HỎI thông tin?";

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final BudgetDao budgetDao;
    private final AiSettings aiSettings;
    private final LlmClient llmClient;
    private final SpeechToTextClient sttClient;
    private final IntentRouter router;
    private final TransactionExtractor transactionExtractor;
    private final PersonalQueryHandler personalQueryHandler;
    private final KnowledgeAnswerer knowledgeAnswerer;
    private final AdviceHandler adviceHandler;
    private final MarketDataHandler marketDataHandler;
    private final TransactionService transactionService;
    private final BudgetCalculator budgetCalculator;
    private final ChatMessageViews views;
    private final JsonSupport json;
    private final Clock clock;

    @Override
    @Transactional
    public ChatResponse send(Long userId, ChatRequest request) {
        return send(userId, request, ReplySink.NONE);
    }

    @Override
    @Transactional
    public ChatResponse send(Long userId, ChatRequest request, ReplySink sink) {
        long started = System.nanoTime();
        AiSettings.Snapshot settings = aiSettings.snapshot();
        if (!llmClient.available() || settings.model() == null || settings.model().isBlank()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }

        Conversation conversation = conversationFor(userId, request.conversationId(), request.message());
        List<ChatMessage> history = history(conversation);
        InputMode mode = request.inputMode() == null ? InputMode.TEXT : request.inputMode();
        Message userMessage = save(conversation, MessageRole.USER, request.message().trim(), mode);

        String text = request.message().trim();
        sink.stage(ReplySink.Stage.THINKING);
        if (router.needsLlm(text)) {
            // Tính embedding cho RAG song song với lượt gọi LLM phân loại ý định (~3s): nếu ý định cần RAG thì đã có sẵn
            knowledgeAnswerer.prefetch(text, history);
        }
        IntentRouter.Decision decision = router.route(text, history, settings);
        HandlerResult result = dispatch(userId, decision.intent(), text, history, settings, sink);
        return respond(conversation, userMessage, result, started);
    }

    @Override
    @Transactional
    public ChatResponse receipt(Long userId, Long conversationId, byte[] image, String contentType) {
        long started = System.nanoTime();
        String mime = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (image == null || image.length == 0 || image.length > MAX_IMAGE_BYTES || !IMAGE_TYPES.contains(mime)) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID);
        }
        AiSettings.Snapshot settings = aiSettings.snapshot();
        if (!llmClient.available() || settings.model() == null || settings.model().isBlank()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }
        Conversation conversation = conversationFor(userId, conversationId, RECEIPT_MESSAGE);
        // Ảnh không được lưu (như BR-09 với âm thanh): tin nhắn chỉ ghi lại là đã gửi ảnh
        Message userMessage = save(conversation, MessageRole.USER, RECEIPT_MESSAGE, InputMode.IMAGE);
        HandlerResult result = transactionExtractor.handleReceipt(userId, image, mime, settings);
        return respond(conversation, userMessage, result, started);
    }

    private ChatResponse respond(Conversation conversation, Message userMessage, HandlerResult result, long started) {
        Message reply = new Message();
        reply.assignConversation(conversation);
        reply.setRole(MessageRole.ASSISTANT);
        reply.setContent(result.content());
        reply.setInputMode(InputMode.TEXT);
        reply.setIntent(result.intent().name());
        reply.setCitations(json.write(result.citations()));
        reply.setPayload(json.write(result.payload()));
        reply.setRetrieval(json.write(result.retrieval()));
        reply.setNoSource(result.noSource());
        reply.setLatencyMs((int) ((System.nanoTime() - started) / 1_000_000));
        messageDao.save(reply);
        touch(conversation);

        return new ChatResponse(conversation.getId(), conversation.getTitle(), views.toView(userMessage), views.toView(reply));
    }

    private HandlerResult dispatch(Long userId, Intent intent, String message, List<ChatMessage> history,
                                   AiSettings.Snapshot settings, ReplySink sink) {
        return switch (intent) {
            case RECORD_TRANSACTION -> transactionExtractor.handle(userId, message, settings);
            case QUERY_PERSONAL_DATA -> personalQueryHandler.handle(userId, message, history, settings, sink);
            case KNOWLEDGE_QA -> knowledgeAnswerer.answer(message, history, settings, sink);
            case PERSONAL_ADVICE -> adviceHandler.handle(userId, message, history, settings, sink);
            case MARKET_DATA -> marketDataHandler.handle(message, settings);
            case SMALL_TALK -> HandlerResult.text(Intent.SMALL_TALK, SMALL_TALK_REPLY);
            case OUT_OF_SCOPE -> HandlerResult.text(Intent.OUT_OF_SCOPE, OUT_OF_SCOPE_REPLY);
            case CLARIFY -> {
                ObjectNode payload = json.object();
                payload.put("type", "CLARIFY");
                payload.putArray("options").add("RECORD_TRANSACTION").add("ASK_QUESTION");
                yield HandlerResult.withPayload(Intent.CLARIFY, CLARIFY_REPLY, payload);
            }
        };
    }

    @Override
    @Transactional
    public ConfirmTransactionsResponse confirmTransactions(Long userId, ConfirmTransactionsRequest request) {
        TransactionSource source = switch (request.inputMode() == null ? InputMode.TEXT : request.inputMode()) {
            case VOICE -> TransactionSource.VOICE;
            case IMAGE -> TransactionSource.RECEIPT;
            case TEXT -> TransactionSource.CHAT;
        };
        List<TransactionResult> results = new ArrayList<>();
        Set<Long> expenseCategories = new LinkedHashSet<>();
        LocalDate latest = null;
        for (TransactionRequest t : request.transactions()) {
            TransactionResult r = transactionService.create(userId, t, source);
            results.add(r);
            if (r.transaction().type() == TransactionType.EXPENSE && r.transaction().categoryId() != null) {
                expenseCategories.add(r.transaction().categoryId());
                latest = r.transaction().txnDate();
            }
        }

        StringBuilder sb = new StringBuilder(results.size() == 1 ? "Đã lưu giao dịch." : "Đã lưu %d giao dịch.".formatted(results.size()));
        if (latest != null) {
            String month = Months.of(latest);
            for (Long categoryId : expenseCategories) {
                budgetDao.findByCategoryAndMonth(userId, categoryId, month).ifPresent(b -> {
                    var usage = budgetCalculator.usage(b);
                    sb.append("\nNgân sách ").append(b.getCategory().getName()).append(" tháng này còn ")
                            .append(MoneyFormatter.format(usage.remaining()))
                            .append(" (đã dùng ").append(usage.percentUsed()).append("%).");
                });
            }
        }
        if (results.stream().anyMatch(TransactionResult::negativeBalance)) {
            sb.append("\nLưu ý: số dư ví đang âm.");
        }

        Message reply = null;
        if (request.conversationId() != null) {
            Conversation conversation = loadConversation(userId, request.conversationId());
            reply = new Message();
            reply.assignConversation(conversation);
            reply.setRole(MessageRole.ASSISTANT);
            reply.setContent(sb.toString());
            reply.setInputMode(InputMode.TEXT);
            reply.setIntent(Intent.RECORD_TRANSACTION.name());
            ObjectNode payload = json.object();
            payload.put("type", "TRANSACTIONS_SAVED");
            var ids = payload.putArray("transactionIds");
            results.forEach(r -> ids.add(r.transaction().id()));
            reply.setPayload(json.write(payload));
            messageDao.save(reply);
            touch(conversation);
        }
        return new ConfirmTransactionsResponse(results, reply == null ? null
                : views.toView(reply));
    }

    @Override
    public TranscriptionResponse transcribe(byte[] audio, String filename) {
        if (audio == null || audio.length == 0 || audio.length > MAX_AUDIO_BYTES) {
            throw new BusinessException(ErrorCode.AUDIO_INVALID);
        }
        if (!sttClient.sttAvailable()) {
            throw new BusinessException(ErrorCode.STT_NOT_CONFIGURED);
        }
        String language = aiSettings.snapshot().sttLanguage();
        String iso = language == null ? "vi" : language.split("-")[0].toLowerCase(Locale.ROOT);
        String text = sttClient.transcribe(audio, filename == null ? "audio.webm" : filename, iso);
        return new TranscriptionResponse(text == null ? "" : text.trim());
    }

    // ---------- nội bộ ----------

    private Conversation conversationFor(Long userId, Long conversationId, String firstMessage) {
        if (conversationId != null) {
            Conversation c = loadConversation(userId, conversationId);
            if (ConversationServiceImpl.DEFAULT_TITLE.equals(c.getTitle())) {
                c.setTitle(title(firstMessage));
            }
            return c;
        }
        Conversation c = new Conversation();
        c.setUserId(userId);
        c.setTitle(title(firstMessage));
        return conversationDao.save(c);
    }

    /** Tiêu đề tự sinh từ câu hỏi đầu (UC-B08 bước 1). */
    private static String title(String message) {
        String t = message.trim().replaceAll("\\s+", " ");
        return t.length() <= TITLE_LENGTH ? t : t.substring(0, TITLE_LENGTH - 1) + "…";
    }

    private List<ChatMessage> history(Conversation conversation) {
        if (conversation.getId() == null) {
            return List.of();
        }
        return messageDao.findRecentByConversationId(conversation.getId(), HISTORY_MESSAGES).stream()
                .map(m -> m.getRole() == MessageRole.USER ? ChatMessage.user(m.getContent()) : ChatMessage.assistant(m.getContent()))
                .toList();
    }

    private Message save(Conversation conversation, MessageRole role, String content, InputMode mode) {
        Message m = new Message();
        m.assignConversation(conversation);
        m.setRole(role);
        m.setContent(content);
        m.setInputMode(mode);
        return messageDao.save(m);
    }

    private void touch(Conversation c) {
        c.setUpdatedAt(java.time.LocalDateTime.now(clock));
    }

    private Conversation loadConversation(Long userId, Long conversationId) {
        return conversationDao.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
    }
}
