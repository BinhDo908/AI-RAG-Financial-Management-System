package com.fintech.chatbot.ai.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fintech.chatbot.ai.AiSettings;
import com.fintech.chatbot.ai.AiTask;
import com.fintech.chatbot.ai.Intent;
import com.fintech.chatbot.ai.JsonSupport;
import com.fintech.chatbot.ai.Prompts;
import com.fintech.chatbot.ai.ReplySink;
import com.fintech.chatbot.ai.client.ChatMessage;
import com.fintech.chatbot.ai.client.LlmClient;
import com.fintech.chatbot.ai.rag.Retriever;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * UC-B02 — Tư vấn cá nhân hóa = tóm tắt số liệu (code tính) + kiến thức RAG (có trích dẫn) → LLM.
 */
@Component
@RequiredArgsConstructor
public class AdviceHandler {

    static final String NEED_DATA_HINT = "\n\nĐể lời khuyên sát với bạn hơn, hãy ghi chép thu chi ít nhất một tháng "
            + "hoặc điền Hồ sơ tài chính (thu nhập, mục tiêu, mức chấp nhận rủi ro).";

    private final FinancialSummaryBuilder summaryBuilder;
    private final Retriever retriever;
    private final KnowledgeAnswerer knowledgeAnswerer;
    private final LlmClient llm;
    private final JsonSupport json;

    public HandlerResult handle(Long userId, String question, List<ChatMessage> history, AiSettings.Snapshot settings,
                                ReplySink sink) {
        FinancialSummaryBuilder.Summary summary = summaryBuilder.build(userId);
        if (!summary.hasData()) {
            // AF-1: chưa đủ dữ liệu → trả lời ở mức kiến thức chung + gợi ý ghi chép / điền hồ sơ
            HandlerResult general = knowledgeAnswerer.answer(question, history, settings, sink);
            return new HandlerResult(Intent.PERSONAL_ADVICE, insertBeforeDisclaimer(general.content(), settings),
                    general.citations(), null, general.retrieval(), general.noSource());
        }

        sink.stage(ReplySink.Stage.SEARCHING);
        Retriever.Result retrieval = retriever.retrieve(KnowledgeAnswerer.retrievalQuery(question, history), settings);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(Prompts.ADVICE.formatted(summary.data().toPrettyString(),
                KnowledgeAnswerer.contextBlock(retrieval.chunks()))));
        messages.addAll(history);
        messages.add(ChatMessage.user(question));
        sink.stage(ReplySink.Stage.WRITING);
        String answer = KnowledgeAnswerer.generate(llm, settings.model(AiTask.ADVICE), messages, settings.temperature(), sink);

        ObjectNode payload = json.object();
        payload.put("type", "FINANCIAL_SUMMARY");
        payload.set("summary", summary.data());
        return new HandlerResult(Intent.PERSONAL_ADVICE, KnowledgeAnswerer.withDisclaimer(answer, settings),
                retrieval.chunks().isEmpty() ? null : knowledgeAnswerer.citations(answer, retrieval.chunks()),
                payload, knowledgeAnswerer.retrievalJson(retrieval), false);
    }

    /** Gợi ý ghi chép / điền hồ sơ đặt trước disclaimer để disclaimer luôn là dòng cuối của câu trả lời. */
    private static String insertBeforeDisclaimer(String content, AiSettings.Snapshot settings) {
        String disclaimer = "\n\n_" + settings.disclaimer() + "_";
        if (content.endsWith(disclaimer)) {
            return content.substring(0, content.length() - disclaimer.length()) + NEED_DATA_HINT + disclaimer;
        }
        return content + NEED_DATA_HINT;
    }
}
