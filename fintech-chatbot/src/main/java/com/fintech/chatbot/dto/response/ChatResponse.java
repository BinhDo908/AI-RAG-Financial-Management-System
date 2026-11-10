package com.fintech.chatbot.dto.response;

/**
 * Kết quả gửi một tin nhắn: tin của user và câu trả lời của chatbot.
 */
public record ChatResponse(Long conversationId, String conversationTitle, ChatMessageView userMessage,
                           ChatMessageView reply) {
}
