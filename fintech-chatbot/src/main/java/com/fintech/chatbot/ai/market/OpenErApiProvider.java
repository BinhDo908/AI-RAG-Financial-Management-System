package com.fintech.chatbot.ai.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.config.ClockConfig;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * SRC-12 — open.er-api.com: tỷ giá tham khảo quốc tế (tỷ giá giữa, không phải giá mua / bán của ngân hàng).
 * Endpoint: {@code GET https://open.er-api.com/v6/latest/{BASE}}.
 */
@Component
public class OpenErApiProvider implements ExchangeRateProvider {

    private static final String URL = "https://open.er-api.com/v6/latest/{base}";

    private final RestClient restClient;

    public OpenErApiProvider(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = builder.requestFactory(factory).build();
    }

    @Override
    public String sourceId() {
        return "SRC-12";
    }

    @Override
    public String sourceName() {
        return "open.er-api.com (tỷ giá tham khảo quốc tế)";
    }

    @Override
    public Quote fetch(String base, String quote) {
        try {
            JsonNode body = restClient.get().uri(URL, base).retrieve().body(JsonNode.class);
            if (body == null || !"success".equals(body.path("result").asText())) {
                throw new BusinessException(ErrorCode.MARKET_DATA_UNAVAILABLE);
            }
            JsonNode rate = body.path("rates").path(quote);
            if (!rate.isNumber()) {
                throw new BusinessException(ErrorCode.MARKET_DATA_UNAVAILABLE, "Không có tỷ giá " + base + "/" + quote);
            }
            long updatedUnix = body.path("time_last_update_unix").asLong(Instant.now().getEpochSecond());
            return new Quote(rate.decimalValue(),
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(updatedUnix), ClockConfig.ZONE));
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.MARKET_DATA_UNAVAILABLE);
        }
    }
}
