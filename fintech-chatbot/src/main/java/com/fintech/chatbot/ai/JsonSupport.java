package com.fintech.chatbot.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Đọc JSON từ câu trả lời LLM (có thể kèm ```json … ``` hoặc chữ thừa) và ghi JSON để lưu vào DB.
 */
@Component
@RequiredArgsConstructor
public class JsonSupport {

    private final ObjectMapper objectMapper;

    /** Lấy object JSON đầu tiên trong văn bản; rỗng nếu không parse được. */
    public Optional<JsonNode> extractObject(String text) {
        if (text == null) {
            return Optional.empty();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(text.substring(start, end + 1));
            return node.isObject() ? Optional.of(node) : Optional.empty();
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON", e);
        }
    }

    public JsonNode read(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public ObjectNode object() {
        return objectMapper.createObjectNode();
    }

    public JsonNode toTree(Object value) {
        return objectMapper.valueToTree(value);
    }

    /** Đọc chuỗi, coi "null" / rỗng là không có. */
    public static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        String s = v.asText().trim();
        return s.isEmpty() || s.equalsIgnoreCase("null") ? null : s;
    }
}
