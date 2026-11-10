package com.fintech.chatbot.ai;

/**
 * Nơi nhận tiến trình trả lời để đẩy dần lên khung chat (SSE): bước đang làm và từng đoạn chữ LLM vừa sinh.
 * Câu trả lời đầy đủ (kèm trích dẫn, disclaimer) vẫn được trả về ở cuối như khi không stream.
 */
public interface ReplySink {

    /** Bước xử lý hiện tại, để giao diện hiện "Đang tìm tài liệu…". */
    enum Stage {
        THINKING, SEARCHING, WRITING
    }

    void stage(Stage stage);

    void delta(String text);

    default boolean streaming() {
        return true;
    }

    /** Không stream (API thường, đánh giá chatbot). */
    ReplySink NONE = new ReplySink() {
        @Override
        public void stage(Stage stage) {
        }

        @Override
        public void delta(String text) {
        }

        @Override
        public boolean streaming() {
            return false;
        }
    };
}
