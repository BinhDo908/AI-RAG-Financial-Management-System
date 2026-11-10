package com.fintech.chatbot.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.config.properties.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Gọi API tương thích OpenAI (mặc định Vilao AI) bằng HTTP thuần:
 * {@code POST /chat/completions}, {@code POST /embeddings}, {@code POST /audio/transcriptions}.
 *
 * <p>Không log nội dung prompt hay API key (NFR-05).
 */
@Slf4j
@Component
public class OpenAiCompatibleClient implements LlmClient, EmbeddingClient, SpeechToTextClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiProperties props;
    private final RestClient restClient;
    /** Các lượt gọi chạy song song (yêu cầu chính + yêu cầu dự phòng). */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public OpenAiCompatibleClient(AiProperties props, RestClient.Builder builder) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds()));
        this.restClient = builder.requestFactory(factory).build();
    }

    private int timeoutSeconds() {
        return Math.max(props.timeoutSeconds(), 5);
    }

    // ---------- Chat ----------

    /**
     * Gửi yêu cầu dự phòng song song (hedged request): model chính chưa trả lời sau {@code hedge-after-ms} thì gửi
     * thêm một yêu cầu tới {@code fallback-model} (nhà cung cấp khác), lấy kết quả nào về trước. Model chính lỗi tạm
     * thời (5xx, quá hạn, phản hồi rỗng kiểu EMPTY_OUTPUT) thì chuyển ngay sang model dự phòng. Tốc độ từng nhà
     * cung cấp trên Vilao dao động theo giờ nên cắt đuôi chậm thay vì chờ. Lỗi key / quyền không thử lại.
     */
    @Override
    public String chat(String model, List<ChatMessage> messages, double temperature) {
        AiProperties.Endpoint ep = props.chat();
        requireUsable(ep, model);
        return hedged(model, (m, attempt) -> complete(ep, m, messages, temperature));
    }

    /**
     * Stream câu trả lời (SSE của nhà cung cấp). Dự phòng như {@link #chat}, nhưng tính theo đoạn chữ đầu tiên: yêu cầu
     * nào ra chữ trước thì được đẩy lên khung chat, yêu cầu còn lại bị hủy — người dùng không thấy hai câu trả lời trộn nhau.
     */
    @Override
    public String stream(String model, List<ChatMessage> messages, double temperature, Consumer<String> onDelta) {
        AiProperties.Endpoint ep = props.chat();
        requireUsable(ep, model);
        AtomicReference<Attempt> owner = new AtomicReference<>();
        return hedged(model, (m, attempt) -> streamOnce(ep, m, messages, temperature, text -> {
            if (owner.compareAndSet(null, attempt) || owner.get() == attempt) {
                attempt.started().complete(null);
                onDelta.accept(text);
            } else {
                throw new LostRace();
            }
        }));
    }

    /** Một lượt gọi; {@code started} hoàn tất khi đã có chữ đầu tiên (chỉ dùng cho stream). */
    private record Attempt(String model, CompletableFuture<Void> started) {
    }

    /** Lượt gọi bị bỏ vì lượt kia đã ra chữ trước. */
    private static final class LostRace extends RuntimeException {
        LostRace() {
            super(null, null, false, false);
        }
    }

    private String hedged(String model, BiFunction<String, Attempt, String> call) {
        String backup = props.hasFallbackModel() ? props.fallbackModel().trim() : model;
        Attempt first = new Attempt(model, new CompletableFuture<>());
        CompletableFuture<String> primary = CompletableFuture.supplyAsync(() -> call.apply(model, first), executor);
        int hedgeAfter = props.hedgeAfterMs();
        if (hedgeAfter <= 0) {
            return await(primary);
        }
        try {
            // Chờ tới khi xong, hoặc (stream) đã ra chữ đầu tiên, hoặc hết thời gian chờ
            CompletableFuture.anyOf(primary, first.started()).get(hedgeAfter, TimeUnit.MILLISECONDS);
            return await(primary);
        } catch (TimeoutException e) {
            log.info("Model {} chưa trả lời sau {}ms, gửi thêm yêu cầu dự phòng tới {}", model, hedgeAfter, backup);
        } catch (ExecutionException e) {
            BusinessException error = unwrap(e);
            if (error.getErrorCode() != ErrorCode.AI_PROVIDER_ERROR) {
                throw error;
            }
            log.warn("AI provider lỗi tạm thời với model {}, gọi lại bằng {}", model, backup);
            return call.apply(backup, new Attempt(backup, new CompletableFuture<>()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
        }
        Attempt second = new Attempt(backup, new CompletableFuture<>());
        CompletableFuture<String> secondary = CompletableFuture.supplyAsync(() -> call.apply(backup, second), executor);
        return await(firstSuccess(primary, secondary));
    }

    /** Kết quả thành công đầu tiên; chỉ lỗi khi cả hai đều lỗi (ưu tiên báo lỗi của model chính). */
    private static CompletableFuture<String> firstSuccess(CompletableFuture<String> primary,
                                                         CompletableFuture<String> secondary) {
        CompletableFuture<String> result = new CompletableFuture<>();
        AtomicInteger failures = new AtomicInteger();
        primary.whenComplete((v, ex) -> {
            if (ex == null) {
                result.complete(v);
            } else if (failures.incrementAndGet() == 2) {
                result.completeExceptionally(ex);
            }
        });
        secondary.whenComplete((v, ex) -> {
            if (ex == null) {
                result.complete(v);
            } else if (failures.incrementAndGet() == 2) {
                result.completeExceptionally(primary.isCompletedExceptionally() ? primary.exceptionNow() : ex);
            }
        });
        return result;
    }

    private String await(CompletableFuture<String> future) {
        try {
            return future.get(timeoutSeconds() * 2L, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw unwrap(e);
        } catch (TimeoutException e) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
        }
    }

    private static BusinessException unwrap(Throwable e) {
        Throwable cause = e;
        while ((cause instanceof ExecutionException || cause instanceof CompletionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof BusinessException b) {
            return b;
        }
        log.warn("Lỗi khi gọi AI provider: {}", cause.getClass().getSimpleName());
        return new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
    }

    /** Tin nhắn dạng OpenAI có phần chữ + ảnh (data URL base64). Ảnh chỉ gửi đi, không lưu lại (như BR-09 với âm thanh). */
    @Override
    public String vision(String model, String system, String text, byte[] image, String mimeType, double temperature) {
        AiProperties.Endpoint ep = props.chat();
        requireUsable(ep, model);
        String dataUrl = "data:" + mimeType + ";base64," + java.util.Base64.getEncoder().encodeToString(image);
        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text", text),
                        Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)))));
        return hedged(model, (m, attempt) -> complete(ep, m, messages, temperature));
    }

    private String complete(AiProperties.Endpoint ep, String model, List<?> messages, double temperature) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);
        JsonNode response = post(ep, "/chat/completions", body);
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull() || content.asText().isBlank()) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
        }
        return content.asText();
    }

    private String streamOnce(AiProperties.Endpoint ep, String model, List<ChatMessage> messages, double temperature,
                              Consumer<String> onDelta) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("stream", true);
        long started = System.nanoTime();
        try {
            String text = restClient.post()
                    .uri(url(ep, "/chat/completions"))
                    .headers(h -> auth(h, ep))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(body)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 401 || status == 403) {
                            log.warn("AI provider /chat/completions (stream) trả lỗi HTTP {}", status);
                            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED, "API key AI không hợp lệ hoặc hết hạn");
                        }
                        if (status >= 400) {
                            log.warn("AI provider /chat/completions (stream) trả lỗi HTTP {}", status);
                            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
                        }
                        return readStream(response.getBody(), onDelta);
                    });
            if (text == null || text.isBlank()) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
            }
            return text;
        } catch (RestClientException e) {
            if (e.getCause() instanceof LostRace lost) {
                throw lost; // yêu cầu thua cuộc bị hủy chủ động, không phải lỗi nhà cung cấp
            }
            throw providerError("/chat/completions", e);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("AI /chat/completions (stream) model={} {}ms", model, (System.nanoTime() - started) / 1_000_000);
            }
        }
    }

    /** Đọc từng dòng "data: {...}" của SSE, ghép các đoạn {@code choices[0].delta.content}. */
    private static String readStream(InputStream in, Consumer<String> onDelta) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                if (data.isEmpty()) {
                    continue;
                }
                String delta = MAPPER.readTree(data).path("choices").path(0).path("delta").path("content").asText("");
                if (!delta.isEmpty()) {
                    onDelta.accept(delta);
                    text.append(delta);
                }
            }
        }
        return text.toString();
    }

    @Override
    public boolean available() {
        return props.chat().usable();
    }

    // ---------- Embedding ----------

    @Override
    public List<float[]> embed(List<String> texts) {
        AiProperties.Endpoint ep = props.embedding();
        requireUsable(ep, ep.model());
        JsonNode response = post(ep, "/embeddings", Map.of("model", ep.model(), "input", texts));
        JsonNode data = response.path("data");
        float[][] vectors = new float[texts.size()][];
        int position = 0;
        for (JsonNode item : data) {
            int index = item.path("index").asInt(position);
            position++;
            if (index < 0 || index >= vectors.length) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
            }
            JsonNode values = item.path("embedding");
            float[] v = new float[values.size()];
            for (int j = 0; j < v.length; j++) {
                v[j] = (float) values.get(j).asDouble();
            }
            vectors[index] = v;
        }
        List<float[]> result = new ArrayList<>(vectors.length);
        for (float[] v : vectors) {
            if (v == null || v.length == 0) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
            }
            result.add(v);
        }
        return result;
    }

    @Override
    public String modelName() {
        return props.embedding().model();
    }

    @Override
    public boolean embeddingAvailable() {
        return props.embedding().usable() && props.embedding().hasModel();
    }

    // ---------- Speech to text ----------

    @Override
    public String transcribe(byte[] audio, String filename, String language) {
        AiProperties.Endpoint ep = props.stt();
        if (!ep.usable() || !ep.hasModel()) {
            throw new BusinessException(ErrorCode.STT_NOT_CONFIGURED);
        }
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("model", ep.model());
        form.add("language", language);
        form.add("file", new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        try {
            JsonNode response = restClient.post()
                    .uri(url(ep, "/audio/transcriptions"))
                    .headers(h -> auth(h, ep))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            return response == null ? "" : response.path("text").asText("");
        } catch (RestClientException e) {
            throw providerError("audio/transcriptions", e);
        }
    }

    @Override
    public boolean sttAvailable() {
        return props.stt().usable() && props.stt().hasModel();
    }

    // ---------- tiện ích ----------

    private JsonNode post(AiProperties.Endpoint ep, String path, Object body) {
        long started = System.nanoTime();
        try {
            JsonNode node = restClient.post()
                    .uri(url(ep, path))
                    .headers(h -> auth(h, ep))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (node == null) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
            }
            return node;
        } catch (RestClientException e) {
            throw providerError(path, e);
        } finally {
            // Chỉ ghi model và thời gian, không ghi nội dung (NFR-05)
            if (log.isDebugEnabled() && body instanceof Map<?, ?> map) {
                log.debug("AI {} model={} {}ms", path, map.get("model"), (System.nanoTime() - started) / 1_000_000);
            }
        }
    }

    private static void requireUsable(AiProperties.Endpoint ep, String model) {
        if (!ep.usable() || model == null || model.isBlank()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }
    }

    private static void auth(HttpHeaders headers, AiProperties.Endpoint ep) {
        if (ep.hasKey()) {
            headers.setBearerAuth(ep.apiKey());
        }
    }

    private static String url(AiProperties.Endpoint ep, String path) {
        String base = ep.baseUrl().endsWith("/") ? ep.baseUrl().substring(0, ep.baseUrl().length() - 1) : ep.baseUrl();
        return base + path;
    }

    private static BusinessException providerError(String path, RestClientException e) {
        if (e instanceof RestClientResponseException r) {
            log.warn("AI provider {} trả lỗi HTTP {}", path, r.getStatusCode().value());
            if (r.getStatusCode().value() == 401 || r.getStatusCode().value() == 403) {
                return new BusinessException(ErrorCode.AI_NOT_CONFIGURED, "API key AI không hợp lệ hoặc hết hạn");
            }
        } else {
            log.warn("Không gọi được AI provider {}: {}", path, e.getClass().getSimpleName());
        }
        return new BusinessException(ErrorCode.AI_PROVIDER_ERROR);
    }
}
