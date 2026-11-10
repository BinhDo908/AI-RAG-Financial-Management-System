package com.fintech.chatbot.ai.client;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.config.properties.AiProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Yêu cầu dự phòng song song (hedged request) và stream. Dùng server HTTP giả trên máy, không gọi API thật.
 * Model giả: fast (trả ngay), medium (1,5s), slow (3s), broken (503), empty (nội dung rỗng), forbidden (403).
 */
class OpenAiCompatibleClientTest {

    private static final Pattern MODEL = Pattern.compile("\"model\"\\s*:\\s*\"([^\"]+)\"");
    private static final int HEDGE_AFTER_MS = 1000;

    private HttpServer server;
    private final List<String> calls = new CopyOnWriteArrayList<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/v1/chat/completions", this::handle);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Matcher m = MODEL.matcher(body);
        String model = m.find() ? m.group(1) : "?";
        boolean stream = body.contains("\"stream\":true");
        calls.add(model);
        switch (model) {
            case "medium" -> sleep(1500);
            case "slow" -> sleep(3000);
            case "broken" -> {
                respond(exchange, 503, "{}");
                return;
            }
            case "forbidden" -> {
                respond(exchange, 403, "{}");
                return;
            }
            default -> {
            }
        }
        String content = "empty".equals(model) ? "" : "ok-" + model;
        if (!stream) {
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"" + content + "\"}}]}");
            return;
        }
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream out = exchange.getResponseBody()) {
            for (String part : content.isEmpty() ? List.<String>of() : List.of("ok-", model)) {
                out.write(("data: {\"choices\":[{\"delta\":{\"content\":\"" + part + "\"}}]}\n\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // Client đã hủy (yêu cầu thua cuộc)
        }
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private OpenAiCompatibleClient client(String fallbackModel) {
        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
        AiProperties.Endpoint ep = new AiProperties.Endpoint(base, "test-key", "fast");
        AiProperties props = new AiProperties(10, HEDGE_AFTER_MS, fallbackModel, ep, ep, ep, null);
        return new OpenAiCompatibleClient(props, RestClient.builder());
    }

    private static List<ChatMessage> hello() {
        return List.of(ChatMessage.user("xin chào"));
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    @Test
    void fastModelDoesNotTriggerBackupRequest() {
        assertThat(client("medium").chat("fast", hello(), 0)).isEqualTo("ok-fast");
        assertThat(calls).containsExactly("fast");
    }

    @Test
    void slowModelIsHedgedAndBackupWins() {
        long t0 = System.nanoTime();
        String answer = client("fast").chat("slow", hello(), 0);

        assertThat(answer).isEqualTo("ok-fast");
        assertThat(calls).containsExactly("slow", "fast");
        assertThat(elapsedMs(t0)).isLessThan(2500); // không chờ đủ 3s của model chậm
    }

    @Test
    void primaryStillWinsWhenBackupIsSlower() {
        // medium trả lời ở 1,5s; yêu cầu dự phòng (slow) gửi ở 1s nhưng 3s mới xong
        assertThat(client("slow").chat("medium", hello(), 0)).isEqualTo("ok-medium");
        assertThat(calls).containsExactly("medium", "slow");
    }

    @Test
    void transientErrorsSwitchToBackupImmediately() {
        long t0 = System.nanoTime();
        assertThat(client("fast").chat("broken", hello(), 0)).isEqualTo("ok-fast");
        assertThat(client("fast").chat("empty", hello(), 0)).isEqualTo("ok-fast");
        assertThat(elapsedMs(t0)).isLessThan(HEDGE_AFTER_MS); // không chờ tới mốc gửi dự phòng
    }

    @Test
    void withoutBackupModelRetriesSameModel() {
        assertThatThrownBy(() -> client("").chat("broken", hello(), 0))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_PROVIDER_ERROR);
        assertThat(calls).containsExactly("broken", "broken");
    }

    @Test
    void authErrorIsNotRetried() {
        assertThatThrownBy(() -> client("fast").chat("forbidden", hello(), 0))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_NOT_CONFIGURED);
        assertThat(calls).containsExactly("forbidden");
    }

    @Test
    void streamPushesDeltasInOrder() {
        List<String> deltas = new CopyOnWriteArrayList<>();
        assertThat(client("medium").stream("fast", hello(), 0, deltas::add)).isEqualTo("ok-fast");
        assertThat(deltas).containsExactly("ok-", "fast");
    }

    @Test
    void streamOnlyForwardsTheRequestThatProducesTextFirst() throws InterruptedException {
        List<String> deltas = new CopyOnWriteArrayList<>();
        long t0 = System.nanoTime();
        String answer = client("fast").stream("slow", hello(), 0, deltas::add);

        assertThat(answer).isEqualTo("ok-fast");
        assertThat(elapsedMs(t0)).isLessThan(2500);
        Thread.sleep(2500); // chờ yêu cầu chậm trả về: chữ của nó không được trộn vào câu trả lời
        assertThat(deltas).containsExactly("ok-", "fast");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
