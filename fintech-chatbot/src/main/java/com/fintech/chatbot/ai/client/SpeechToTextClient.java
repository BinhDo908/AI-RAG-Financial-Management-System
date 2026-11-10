package com.fintech.chatbot.ai.client;

/**
 * Chuyển giọng nói thành văn bản ở server (UC-A12, B03 — phương án 2).
 * Phương án 1 (Web Speech API) chạy ở trình duyệt nên không qua interface này.
 */
public interface SpeechToTextClient {

    /**
     * @param audio    nội dung file ghi âm; không được lưu lại sau khi chuyển (BR-09)
     * @param language mã ngôn ngữ ISO-639-1, ví dụ "vi"
     */
    String transcribe(byte[] audio, String filename, String language);

    boolean sttAvailable();
}
