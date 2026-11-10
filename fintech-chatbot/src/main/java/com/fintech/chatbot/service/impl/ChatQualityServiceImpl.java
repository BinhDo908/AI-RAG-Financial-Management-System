package com.fintech.chatbot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintech.chatbot.ai.AiSettings;
import com.fintech.chatbot.ai.AiTask;
import com.fintech.chatbot.ai.Intent;
import com.fintech.chatbot.ai.JsonSupport;
import com.fintech.chatbot.ai.Prompts;
import com.fintech.chatbot.ai.client.ChatMessage;
import com.fintech.chatbot.ai.client.LlmClient;
import com.fintech.chatbot.ai.handler.HandlerResult;
import com.fintech.chatbot.ai.handler.KnowledgeAnswerer;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.EvalRunDao;
import com.fintech.chatbot.dao.FeedbackDao;
import com.fintech.chatbot.dao.MessageDao;
import com.fintech.chatbot.dto.request.EvaluationRequest;
import com.fintech.chatbot.dto.response.ChatQualityResponses;
import com.fintech.chatbot.entity.EvalRun;
import com.fintech.chatbot.entity.Message;
import com.fintech.chatbot.entity.enums.FeedbackRating;
import com.fintech.chatbot.entity.enums.InputMode;
import com.fintech.chatbot.service.ChatQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatQualityServiceImpl implements ChatQualityService {

    private static final int DEFAULT_DAYS = 30;
    private static final int UNANSWERED_LIMIT = 20;
    private static final int RUNS_LIMIT = 20;

    private final MessageDao messageDao;
    private final FeedbackDao feedbackDao;
    private final EvalRunDao evalRunDao;
    private final KnowledgeAnswerer knowledgeAnswerer;
    private final AiSettings aiSettings;
    private final LlmClient llm;
    private final JsonSupport json;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ChatQualityResponses.Dashboard dashboard(LocalDateTime from, LocalDateTime to) {
        LocalDateTime end = to == null ? LocalDateTime.now(clock) : to;
        LocalDateTime start = from == null ? end.minusDays(DEFAULT_DAYS) : from;

        Map<String, Long> intents = messageDao.countAnswersByIntent(start, end);
        Map<InputMode, Long> modes = messageDao.countUserMessagesByInputMode(start, end);
        long voice = modes.getOrDefault(InputMode.VOICE, 0L);
        long totalInputs = modes.values().stream().mapToLong(Long::longValue).sum();
        Map<FeedbackRating, Long> ratings = feedbackDao.countByRating(start, end);
        long up = ratings.get(FeedbackRating.UP);
        long down = ratings.get(FeedbackRating.DOWN);
        long knowledge = intents.getOrDefault(Intent.KNOWLEDGE_QA.name(), 0L)
                + intents.getOrDefault(Intent.PERSONAL_ADVICE.name(), 0L);

        List<Message> noSource = messageDao.findNoSourceAnswers(start, end, UNANSWERED_LIMIT);
        long noSourceCount = messageDao.countNoSourceAnswers(start, end);
        List<String> questions = noSource.stream()
                .map(m -> json.read(m.getRetrieval()))
                .filter(Objects::nonNull)
                .map(r -> JsonSupport.text(r, "question"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        var latest = evalRunDao.findRecent(1).stream().findFirst().map(this::view).orElse(null);
        return new ChatQualityResponses.Dashboard(start, end,
                messageDao.countActiveUsers(start, end), messageDao.countConversations(start, end),
                messageDao.countUserMessages(start, end), intents,
                ratio(voice, totalInputs), up, down, ratio(up, up + down),
                knowledge, noSourceCount, ratio(noSourceCount, knowledge), questions,
                messageDao.averageLatencyMs(start, end), latest);
    }

    @Override
    @Transactional
    public ChatQualityResponses.EvalRunDetail evaluate(Long adminId, EvaluationRequest request) {
        AiSettings.Snapshot settings = aiSettings.snapshot();
        if (!llm.available() || settings.model() == null || settings.model().isBlank()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }
        int hits = 0;
        int cited = 0;
        int correct = 0;
        ArrayNode details = json.object().arrayNode();
        for (EvaluationRequest.Item item : request.items()) {
            HandlerResult r = knowledgeAnswerer.answer(item.question(), List.of(), settings);
            boolean hit = isHit(r.retrieval(), item);
            boolean hasCitation = r.citations() != null && !r.citations().isEmpty();
            boolean isCorrect = !r.noSource() && judge(item, r.content(), settings);
            hits += hit ? 1 : 0;
            cited += hasCitation ? 1 : 0;
            correct += isCorrect ? 1 : 0;

            ObjectNode d = details.addObject();
            d.put("question", item.question());
            d.put("expectedAnswer", item.expectedAnswer());
            d.put("answer", r.content());
            d.put("hit", hit);
            d.put("cited", hasCitation);
            d.put("correct", isCorrect);
            d.put("noSource", r.noSource());
        }
        int n = request.items().size();
        EvalRun run = new EvalRun();
        run.setDatasetName(request.datasetName());
        run.setTotalQuestions(n);
        run.setHitRate(ratio(hits, n));
        run.setCitationRate(ratio(cited, n));
        run.setAccuracy(ratio(correct, n));
        run.setConfigSnapshot(json.write(Map.of("models", settings.taskModels(), "topK", settings.topK(),
                "topN", settings.effectiveTopN(), "scoreThreshold", settings.scoreThreshold(),
                "temperature", settings.temperature())));
        run.setDetails(json.write(details));
        run.setRunBy(adminId);
        evalRunDao.save(run);
        return new ChatQualityResponses.EvalRunDetail(view(run), details);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatQualityResponses.EvalRunView> evaluations() {
        return evalRunDao.findRecent(RUNS_LIMIT).stream().map(this::view).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatQualityResponses.EvalRunDetail evaluation(Long id) {
        EvalRun run = evalRunDao.findById(id).orElseThrow(() -> BusinessException.notFound("lần đánh giá"));
        return new ChatQualityResponses.EvalRunDetail(view(run), json.read(run.getDetails()));
    }

    /** Hit@k: tài liệu (hoặc nguồn) mong đợi có trong các đoạn top_k truy xuất được. */
    private static boolean isHit(JsonNode retrieval, EvaluationRequest.Item item) {
        if (retrieval == null) {
            return false;
        }
        for (JsonNode c : retrieval.path("candidates")) {
            boolean match = item.expectedDocumentId() != null
                    ? item.expectedDocumentId() == c.path("documentId").asLong()
                    : item.expectedSourceId() != null && item.expectedSourceId().equals(c.path("sourceId").asText());
            if (match) {
                return true;
            }
        }
        return false;
    }

    private boolean judge(EvaluationRequest.Item item, String answer, AiSettings.Snapshot settings) {
        String raw = llm.chat(settings.model(AiTask.JUDGE), List.of(ChatMessage.user(
                Prompts.JUDGE.formatted(item.question(), item.expectedAnswer(), answer))), 0);
        return json.extractObject(raw).map(n -> n.path("correct").asBoolean(false)).orElse(false);
    }

    private ChatQualityResponses.EvalRunView view(EvalRun r) {
        return new ChatQualityResponses.EvalRunView(r.getId(), r.getDatasetName(), r.getTotalQuestions(), r.getHitRate(),
                r.getCitationRate(), r.getAccuracy(), json.read(r.getConfigSnapshot()), r.getCreatedAt());
    }

    private static double ratio(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 10000.0 / total) / 10000.0;
    }
}
