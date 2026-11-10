package com.fintech.chatbot.ai;

import com.fintech.chatbot.ai.client.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptsTest {

    @Test
    void noHistoryKeepsSystemPromptUnchanged() {
        assertThat(Prompts.withHistory("Chỉ trả về JSON.", List.of())).isEqualTo("Chỉ trả về JSON.");
    }

    @Test
    void historyIsQuotedInsideSystemPrompt() {
        String prompt = Prompts.withHistory("Chỉ trả về JSON.", List.of(
                ChatMessage.user("Tháng này tôi chi bao nhiêu cho ăn uống?"),
                ChatMessage.assistant("Bạn đã chi\n**101.000đ**")));

        assertThat(prompt).startsWith("Chỉ trả về JSON.")
                .contains("- Người dùng: Tháng này tôi chi bao nhiêu cho ăn uống?")
                .contains("- Trợ lý: Bạn đã chi **101.000đ**");
    }

    @Test
    void longHistoryLineIsTruncated() {
        String prompt = Prompts.withHistory("JSON", List.of(ChatMessage.assistant("a".repeat(1000))));

        assertThat(prompt).doesNotContain("a".repeat(301)).contains("a".repeat(300) + "…");
    }
}
