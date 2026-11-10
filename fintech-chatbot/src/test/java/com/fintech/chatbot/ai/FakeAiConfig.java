package com.fintech.chatbot.ai;

import com.fintech.chatbot.ai.client.ChatMessage;
import com.fintech.chatbot.ai.client.EmbeddingClient;
import com.fintech.chatbot.ai.client.LlmClient;
import com.fintech.chatbot.ai.client.SpeechToTextClient;
import com.fintech.chatbot.ai.market.ExchangeRateProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI giả lập cho test: trả lời theo kịch bản dựa vào prompt hệ thống, không cần API key thật.
 */
@TestConfiguration
public class FakeAiConfig {

    /** Từ khóa → một chiều của vector embedding; chiều 0 là hằng số nhỏ. */
    static final List<String> KEYWORDS = List.of("quỹ khẩn cấp", "lãi suất", "thẻ tín dụng", "tiết kiệm", "ngân sách", "bảo hiểm");

    @Bean
    @Primary
    public FakeLlm fakeLlm() {
        return new FakeLlm();
    }

    @Bean
    @Primary
    public FakeEmbedding fakeEmbedding() {
        return new FakeEmbedding();
    }

    @Bean
    @Primary
    public SpeechToTextClient fakeStt() {
        return new SpeechToTextClient() {
            @Override
            public String transcribe(byte[] audio, String filename, String language) {
                return " ăn sáng 30k ";
            }

            @Override
            public boolean sttAvailable() {
                return true;
            }
        };
    }

    @Bean
    @Primary
    public FakeRates fakeRates() {
        return new FakeRates();
    }

    public static class FakeLlm implements LlmClient {

        public final List<List<ChatMessage>> calls = new ArrayList<>();

        /** Ảnh 1 byte = "không phải hóa đơn"; ảnh khác = hóa đơn 3 món, giảm 1.500đ, thanh toán 130.000đ bằng tiền mặt. */
        @Override
        public String vision(String model, String system, String text, byte[] image, String mimeType, double temperature) {
            if (image.length == 1) {
                return "{\"transactions\": []}";
            }
            return """
                    ```json
                    {"merchant": "Siêu thị mini An Khang", "date": "%s", "total": 130000, "discount": 1500,
                     "wallet": "Tiền mặt", "transactions": [
                      {"amount_text": "64.000", "amount": 64000, "category": "Ăn uống", "note": "Sữa tươi"},
                      {"amount_text": "25.000", "amount": 25000, "category": "Ăn uống", "note": "Bánh mì"},
                      {"amount_text": "42.500", "amount": 42500, "category": "Mua sắm", "note": "Nước rửa chén"}]}
                    ```""".formatted(java.time.LocalDate.now().minusDays(2));
        }

        @Override
        public String chat(String model, List<ChatMessage> messages, double temperature) {
            calls.add(messages);
            String system = messages.get(0).content();
            String user = messages.get(messages.size() - 1).content().toLowerCase(Locale.ROOT);
            if (system.startsWith("Bạn là bộ phân loại ý định")) {
                return route(user);
            }
            if (system.startsWith("Bạn trích xuất giao dịch")) {
                return extract(user);
            }
            if (system.startsWith("Bạn chuyển câu hỏi")) {
                return user.contains("ví")
                        ? "{\"entity\":\"wallet\",\"metric\":\"balance\"}"
                        : "```json\n{\"entity\":\"transaction\",\"metric\":\"sum\",\"type\":\"EXPENSE\",\"category\":\"Ăn uống\","
                        + "\"date_from\":null,\"date_to\":null,\"group_by\":null,\"compare_previous\":false}\n```";
            }
            if (system.startsWith("Bạn là trợ lý tài chính cá nhân. Trả lời")) {
                return "PHRASED " + system.substring(system.indexOf("Dữ liệu:"));
            }
            if (system.startsWith("Bạn là trợ lý giáo dục")) {
                return "Quỹ khẩn cấp nên bằng 3 đến 6 tháng chi tiêu thiết yếu [1].";
            }
            if (system.startsWith("Bạn là trợ lý tư vấn")) {
                return "Bạn đang chi khá nhiều cho ăn uống. Hãy đặt ngân sách và trích 10% thu nhập vào quỹ khẩn cấp [1].";
            }
            if (system.startsWith("Trích thông tin")) {
                return "{\"kind\":\"FX\",\"base\":\"USD\",\"quote\":\"VND\",\"amount\":100}";
            }
            if (system.startsWith("Bạn chấm điểm")) {
                return "{\"correct\": true}";
            }
            return "OK";
        }

        @Override
        public boolean available() {
            return true;
        }

        private static String route(String text) {
            String intent;
            double confidence = 0.95;
            if (text.contains("hmm")) {
                intent = "RECORD_TRANSACTION";
                confidence = 0.3;
            } else if (text.contains("chào")) {
                intent = "SMALL_TALK";
            } else if (text.contains("bóng đá")) {
                intent = "OUT_OF_SCOPE";
            } else if (text.contains("đô")) {
                intent = "MARKET_DATA";
            } else if (text.contains("nên tiết kiệm")) {
                intent = "PERSONAL_ADVICE";
            } else if (text.contains("bao nhiêu cho") || text.contains("ví còn")) {
                intent = "QUERY_PERSONAL_DATA";
            } else if (text.contains("?")) {
                intent = "KNOWLEDGE_QA";
            } else {
                intent = "RECORD_TRANSACTION";
            }
            return "{\"intent\":\"%s\",\"confidence\":%s}".formatted(intent, confidence);
        }

        private static String extract(String text) {
            if (text.contains("phở")) {
                return "{\"transactions\":[{\"type\":\"EXPENSE\",\"amount_text\":\"50k\",\"amount\":50000,"
                        + "\"category\":\"Ăn uống\",\"wallet\":null,\"date\":null,\"note\":\"Ăn phở trưa\"},"
                        + "{\"type\":\"INCOME\",\"amount_text\":\"tám mươi nghìn\",\"amount\":80000,"
                        + "\"category\":\"Thưởng\",\"wallet\":\"Tiền mặt\",\"date\":\"2099-01-01\",\"note\":\"Tiền thưởng\"}]}";
            }
            if (text.contains("cà phê")) {
                return "{\"transactions\":[{\"type\":\"EXPENSE\",\"amount_text\":null,\"amount\":null,"
                        + "\"category\":\"Ăn uống\",\"note\":\"Cà phê\"}]}";
            }
            return "xin lỗi tôi không hiểu";
        }
    }

    public static class FakeEmbedding implements EmbeddingClient {

        @Override
        public List<float[]> embed(List<String> texts) {
            List<float[]> result = new ArrayList<>();
            for (String t : texts) {
                String lower = t.toLowerCase(Locale.ROOT);
                float[] v = new float[KEYWORDS.size() + 1];
                v[0] = 0.05f;
                for (int i = 0; i < KEYWORDS.size(); i++) {
                    v[i + 1] = lower.contains(KEYWORDS.get(i)) ? 1f : 0f;
                }
                result.add(v);
            }
            return result;
        }

        @Override
        public String modelName() {
            return "fake-embed";
        }

        @Override
        public boolean embeddingAvailable() {
            return true;
        }
    }

    public static class FakeRates implements ExchangeRateProvider {

        public final AtomicInteger calls = new AtomicInteger();

        @Override
        public String sourceId() {
            return "SRC-12";
        }

        @Override
        public String sourceName() {
            return "nguồn giả lập";
        }

        @Override
        public Quote fetch(String base, String quote) {
            calls.incrementAndGet();
            return new Quote(BigDecimal.valueOf(25_000), LocalDateTime.now());
        }
    }
}
