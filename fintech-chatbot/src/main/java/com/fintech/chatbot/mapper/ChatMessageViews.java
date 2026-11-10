package com.fintech.chatbot.mapper;

import com.fintech.chatbot.ai.JsonSupport;
import com.fintech.chatbot.dto.response.ChatMessageView;
import com.fintech.chatbot.entity.Message;
import com.fintech.chatbot.entity.enums.MessageRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Message → ChatMessageView: giải mã các cột JSON (citations, payload) thành object.
 * Cột retrieval chỉ dành cho Admin (UC-C02), không trả cho user.
 */
@Component
@RequiredArgsConstructor
public class ChatMessageViews {

    private final JsonSupport json;

    public ChatMessageView toView(Message m) {
        boolean assistant = m.getRole() == MessageRole.ASSISTANT;
        return new ChatMessageView(m.getId(), m.getRole(), m.getContent(), assistant ? null : m.getInputMode(),
                m.getIntent(), json.read(m.getCitations()), json.read(m.getPayload()),
                assistant ? m.isNoSource() : null, m.getLatencyMs(), m.getCreatedAt());
    }
}
