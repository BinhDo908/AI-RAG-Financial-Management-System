package com.fintech.chatbot.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintech.chatbot.ai.AiSettings;
import com.fintech.chatbot.ai.AiTask;
import com.fintech.chatbot.ai.Intent;
import com.fintech.chatbot.ai.JsonSupport;
import com.fintech.chatbot.ai.Prompts;
import com.fintech.chatbot.ai.ReplySink;
import com.fintech.chatbot.ai.client.ChatMessage;
import com.fintech.chatbot.ai.client.LlmClient;
import com.fintech.chatbot.ai.rag.QueryEmbeddings;
import com.fintech.chatbot.ai.rag.Retriever;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UC-B01 — Trả lời kiến thức tài chính bằng RAG, có trích dẫn nguồn (BR-02) và disclaimer (BR-01).
 * Dùng chung cho tư vấn cá nhân hóa (UC-B02) và bộ đánh giá chatbot (UC-C03).
 */
@Component
@RequiredArgsConstructor
public class KnowledgeAnswerer {

    public static final String NO_SOURCE_MESSAGE =
            "Mình chưa tìm thấy thông tin này trong nguồn tham khảo của hệ thống. "
                    + "Bạn thử diễn đạt lại câu hỏi cụ thể hơn nhé.";
    private static final Pattern CITATION = Pattern.compile("\\[(\\d{1,2})]");
    /** Câu hỏi nối tiếp ngắn ("còn thẻ tín dụng thì sao?") được ghép với câu hỏi trước khi truy xuất. */
    private static final int SHORT_FOLLOW_UP = 60;

    private final Retriever retriever;
    private final QueryEmbeddings queryEmbeddings;
    private final LlmClient llm;
    private final JsonSupport json;

    public HandlerResult answer(String question, List<ChatMessage> history, AiSettings.Snapshot settings) {
        return answer(question, history, settings, ReplySink.NONE);
    }

    public HandlerResult answer(String question, List<ChatMessage> history, AiSettings.Snapshot settings, ReplySink sink) {
        sink.stage(ReplySink.Stage.SEARCHING);
        Retriever.Result retrieval = retriever.retrieve(retrievalQuery(question, history), settings);
        if (!retrieval.sufficient()) {
            // AF-1: không đủ nguồn → không gọi LLM, không bịa (BR-02)
            return new HandlerResult(Intent.KNOWLEDGE_QA, NO_SOURCE_MESSAGE, null, null, retrievalJson(retrieval), true);
        }
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(Prompts.KNOWLEDGE.formatted(contextBlock(retrieval.chunks()))));
        messages.addAll(history);
        messages.add(ChatMessage.user(question));
        sink.stage(ReplySink.Stage.WRITING);
        String answer = generate(llm, settings.model(AiTask.ANSWER), messages, settings.temperature(), sink);

        boolean refused = answer.contains("chưa tìm thấy thông tin này trong nguồn");
        return new HandlerResult(Intent.KNOWLEDGE_QA, withDisclaimer(answer, settings),
                refused ? null : citations(answer, retrieval.chunks()), null, retrievalJson(retrieval), refused);
    }

    /** Bắt đầu tính embedding của câu truy xuất ở nền (xem {@link com.fintech.chatbot.ai.rag.QueryEmbeddings}). */
    public void prefetch(String question, List<ChatMessage> history) {
        queryEmbeddings.prefetch(retrievalQuery(question, history));
    }

    /** Gọi LLM; khi đang stream thì đẩy từng đoạn chữ lên khung chat. */
    public static String generate(LlmClient llm, String model, List<ChatMessage> messages, double temperature, ReplySink sink) {
        return sink.streaming() ? llm.stream(model, messages, temperature, sink::delta) : llm.chat(model, messages, temperature);
    }

    /** Đoạn tài liệu đánh số [1], [2]… kèm tiêu đề, đề mục, trang để LLM trích dẫn. */
    public static String contextBlock(List<Retriever.RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return "(không có)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            Retriever.RetrievedChunk c = chunks.get(i);
            sb.append('[').append(i + 1).append("] ").append(c.title());
            if (c.heading() != null) {
                sb.append(" — ").append(c.heading());
            }
            if (c.pageNumber() != null) {
                sb.append(" (trang ").append(c.pageNumber()).append(')');
            }
            sb.append('\n').append(c.content()).append("\n\n");
        }
        return sb.toString().trim();
    }

    /**
     * Danh sách nguồn: các đoạn được đánh số trong câu trả lời; nếu LLM quên đánh số thì liệt kê mọi đoạn đã dùng.
     */
    public ArrayNode citations(String answer, List<Retriever.RetrievedChunk> chunks) {
        List<Integer> referenced = new ArrayList<>();
        Matcher m = CITATION.matcher(answer);
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 1 && n <= chunks.size() && !referenced.contains(n)) {
                referenced.add(n);
            }
        }
        if (referenced.isEmpty()) {
            for (int i = 1; i <= chunks.size(); i++) {
                referenced.add(i);
            }
        }
        ArrayNode list = json.object().arrayNode();
        for (int n : referenced) {
            Retriever.RetrievedChunk c = chunks.get(n - 1);
            ObjectNode item = list.addObject();
            item.put("number", n);
            item.put("documentId", c.documentId());
            item.put("chunkId", c.chunkId());
            item.put("title", c.title());
            item.put("heading", c.heading());
            item.put("pageNumber", c.pageNumber());
            item.put("url", c.url());
            item.put("sourceId", c.sourceId());
        }
        return list;
    }

    public JsonNode retrievalJson(Retriever.Result r) {
        ObjectNode node = json.object();
        node.put("question", r.question());
        node.put("threshold", r.threshold());
        node.put("sufficient", r.sufficient());
        ArrayNode list = node.putArray("candidates");
        for (Retriever.RetrievedChunk c : r.candidates()) {
            ObjectNode item = list.addObject();
            item.put("chunkId", c.chunkId());
            item.put("documentId", c.documentId());
            item.put("sourceId", c.sourceId());
            item.put("title", c.title());
            item.put("heading", c.heading());
            item.put("score", c.score());
            item.put("usedInPrompt", r.chunks().stream().anyMatch(u -> u.chunkId().equals(c.chunkId())));
            item.put("preview", c.content().length() > 300 ? c.content().substring(0, 300) + "…" : c.content());
        }
        return node;
    }

    public static String withDisclaimer(String answer, AiSettings.Snapshot settings) {
        return answer.trim() + "\n\n_" + settings.disclaimer() + "_";
    }

    /** Câu dùng để truy xuất: câu hỏi nối tiếp ngắn được ghép với câu hỏi trước của người dùng. */
    public static String retrievalQuery(String question, List<ChatMessage> history) {
        if (question.length() >= SHORT_FOLLOW_UP) {
            return question;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).role())) {
                return history.get(i).content() + " " + question;
            }
        }
        return question;
    }
}
