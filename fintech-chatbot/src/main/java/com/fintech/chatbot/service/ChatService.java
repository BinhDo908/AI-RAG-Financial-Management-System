package com.fintech.chatbot.service;

import com.fintech.chatbot.ai.ReplySink;
import com.fintech.chatbot.dto.request.ChatRequest;
import com.fintech.chatbot.dto.request.ConfirmTransactionsRequest;
import com.fintech.chatbot.dto.response.ChatResponse;
import com.fintech.chatbot.dto.response.ConfirmTransactionsResponse;
import com.fintech.chatbot.dto.response.TranscriptionResponse;

/**
 * Khung chat (USE_CASES.md §9): A11–A13, B01–B03, B07 đều đi qua đây.
 */
public interface ChatService {

    /** Lưu tin của user → Intent Router → bộ xử lý tương ứng → lưu câu trả lời. */
    ChatResponse send(Long userId, ChatRequest request);

    /** Như {@link #send(Long, ChatRequest)} nhưng đẩy tiến trình và từng đoạn câu trả lời cho {@code sink} (SSE). */
    ChatResponse send(Long userId, ChatRequest request, ReplySink sink);

    /** UC-A11 bước 6–7: lưu các thẻ giao dịch user đã xác nhận (nguồn CHAT hoặc VOICE). */
    ConfirmTransactionsResponse confirmTransactions(Long userId, ConfirmTransactionsRequest request);

    /** UC-B03 phương án 2: chuyển giọng nói ở server; file âm thanh không được lưu (BR-09). */
    TranscriptionResponse transcribe(byte[] audio, String filename);

    /** UC-A14 — Quét ảnh hóa đơn thành các thẻ giao dịch nháp (lưu khi user xác nhận, như UC-A11). */
    ChatResponse receipt(Long userId, Long conversationId, byte[] image, String contentType);
}
