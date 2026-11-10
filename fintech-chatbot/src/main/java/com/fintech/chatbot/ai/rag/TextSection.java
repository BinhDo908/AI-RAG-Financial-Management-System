package com.fintech.chatbot.ai.rag;

/**
 * Một phần văn bản trích từ tài liệu, giữ đề mục và số trang để trích dẫn (UC-C01 bước 5).
 */
public record TextSection(String heading, Integer pageNumber, String text) {
}
