package com.fintech.chatbot.ai.rag;

import com.fintech.chatbot.ai.client.EmbeddingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Embedding của câu truy vấn RAG, tính trước được. Khung chat gọi {@link #prefetch} ngay khi nhận tin nhắn, song song
 * với bước phân loại ý định (§9.1); nếu tin nhắn hóa ra cần RAG thì {@link Retriever} lấy lại kết quả đã có thay vì
 * chờ thêm một lượt gọi embedding (~3s). Giữ kết quả của các câu gần nhất (câu hỏi lặp lại không phải embed lại).
 */
@Slf4j
@Component
public class QueryEmbeddings {

    private static final int MAX_ENTRIES = 256;

    private final EmbeddingClient client;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, CompletableFuture<float[]>> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CompletableFuture<float[]>> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public QueryEmbeddings(EmbeddingClient client) {
        this.client = client;
    }

    /** Bắt đầu tính embedding ở nền, không chờ. Lỗi ở đây bị bỏ qua — {@link #get} sẽ thử lại. */
    public void prefetch(String text) {
        if (text != null && !text.isBlank() && client.embeddingAvailable()) {
            future(text);
        }
    }

    /** Embedding của {@code text}: dùng kết quả đã tính trước nếu có, không thì tính ngay. */
    public float[] get(String text) {
        try {
            return future(text).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException r) {
                throw r;
            }
            throw e;
        }
    }

    private CompletableFuture<float[]> future(String text) {
        String key = client.modelName() + '\n' + text;
        synchronized (cache) {
            CompletableFuture<float[]> existing = cache.get(key);
            if (existing != null) {
                return existing;
            }
            CompletableFuture<float[]> created = CompletableFuture.supplyAsync(() -> client.embed(List.of(text)).get(0), executor);
            // Lỗi không được giữ trong cache: lần sau gọi lại API
            created.whenComplete((v, ex) -> {
                if (ex != null) {
                    synchronized (cache) {
                        cache.remove(key, created);
                    }
                }
            });
            cache.put(key, created);
            return created;
        }
    }
}
