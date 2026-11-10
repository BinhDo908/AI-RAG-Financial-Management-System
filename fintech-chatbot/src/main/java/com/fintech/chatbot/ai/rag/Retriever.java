package com.fintech.chatbot.ai.rag;

import com.fintech.chatbot.ai.AiSettings;
import com.fintech.chatbot.ai.client.EmbeddingClient;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.ChunkDao;
import com.fintech.chatbot.dao.ChunkEmbeddingDao;
import com.fintech.chatbot.entity.Chunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UC-B01 bước 3–6 — Truy xuất đoạn tài liệu liên quan:
 * embed câu hỏi → tìm top_k trong tài liệu APPROVED → bỏ đoạn dưới ngưỡng điểm → giữ tối đa top_n.
 *
 * <p>Bước rerank trong USE_CASES.md §9.2 là tùy chọn: nhà cung cấp API hiện tại không có endpoint rerank,
 * nên xếp hạng theo độ tương đồng cosine.
 */
@Component
@RequiredArgsConstructor
public class Retriever {

    private final EmbeddingClient embeddingClient;
    private final QueryEmbeddings queryEmbeddings;
    private final ChunkEmbeddingDao embeddingDao;
    private final ChunkDao chunkDao;

    public record RetrievedChunk(Long chunkId, Long documentId, String title, String url, String sourceId,
                                 String heading, Integer pageNumber, String content, double score) {
    }

    /**
     * @param sufficient có ít nhất một đoạn đạt ngưỡng {@code rag.score_threshold} (BR-02)
     * @param candidates toàn bộ top_k để Admin xem lại khi kiểm duyệt (UC-C02)
     */
    public record Result(String question, List<RetrievedChunk> chunks, List<RetrievedChunk> candidates,
                         boolean sufficient, double threshold) {
    }

    /** Gọi trong transaction của service. */
    public Result retrieve(String question, AiSettings.Snapshot settings) {
        if (!embeddingClient.embeddingAvailable()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED, "Chưa cấu hình model embedding cho RAG");
        }
        float[] query = queryEmbeddings.get(question);
        List<ChunkEmbeddingDao.ScoredChunk> scored =
                embeddingDao.searchApproved(query, embeddingClient.modelName(), settings.topK());

        Map<Long, Chunk> chunks = chunkDao.findByIdsWithDocument(scored.stream().map(ChunkEmbeddingDao.ScoredChunk::chunkId).toList())
                .stream().collect(Collectors.toMap(Chunk::getId, Function.identity()));
        List<RetrievedChunk> candidates = scored.stream()
                .filter(s -> chunks.containsKey(s.chunkId()))
                .map(s -> toRetrieved(chunks.get(s.chunkId()), s.score()))
                .toList();
        // Chỉ đưa vào prompt các đoạn đạt ngưỡng: đoạn điểm thấp là nhiễu và dễ khiến LLM trích dẫn sai
        List<RetrievedChunk> top = candidates.stream()
                .filter(c -> c.score() >= settings.scoreThreshold())
                .limit(settings.effectiveTopN())
                .toList();
        return new Result(question, top, candidates, !top.isEmpty(), settings.scoreThreshold());
    }

    private static RetrievedChunk toRetrieved(Chunk c, double score) {
        var d = c.getDocument();
        return new RetrievedChunk(c.getId(), d.getId(), d.getTitle(), d.getUrl(), d.getSourceId(), c.getHeading(),
                c.getPageNumber(), c.getContent(), Math.round(score * 10000) / 10000.0);
    }
}
