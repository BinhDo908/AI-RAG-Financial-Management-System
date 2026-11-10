package com.fintech.chatbot.controller;

import com.fintech.chatbot.ai.ReplySink;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.ChatRequest;
import com.fintech.chatbot.dto.request.ConfirmTransactionsRequest;
import com.fintech.chatbot.dto.request.FeedbackRequest;
import com.fintech.chatbot.dto.response.ChatResponse;
import com.fintech.chatbot.dto.response.ConfirmTransactionsResponse;
import com.fintech.chatbot.dto.response.FeedbackResponses;
import com.fintech.chatbot.dto.response.TranscriptionResponse;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.ChatService;
import com.fintech.chatbot.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Tag(name = "Chat", description = "UC-A11–A13, B01–B03, B07 qua Intent Router; UC-B09 đánh giá câu trả lời")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    /** Đủ cho lượt chậm nhất (phân loại + truy xuất + trả lời, mỗi bước có thể gọi lại model dự phòng). */
    private static final long STREAM_TIMEOUT_MS = 180_000;
    private static final ExecutorService STREAM_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final ChatService chatService;
    private final FeedbackService feedbackService;

    @Operation(summary = "Gửi tin nhắn (gõ hoặc transcript giọng nói với inputMode = VOICE)",
            description = "conversationId để trống = hội thoại mới. Ý định RECORD_TRANSACTION trả về thẻ nháp "
                    + "(payload.type = TRANSACTION_DRAFTS), user xác nhận qua POST /api/chat/transactions.")
    @PostMapping("/messages")
    public ApiResponse<ChatResponse> send(@CurrentUser UserPrincipal me, @Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(chatService.send(me.id(), request));
    }

    @Operation(summary = "Gửi tin nhắn, nhận câu trả lời dạng stream (Server-Sent Events)",
            description = "Sự kiện: stage {stage: THINKING|SEARCHING|WRITING}, delta {text} — đoạn chữ vừa sinh, "
                    + "done — ChatResponse đầy đủ như POST /api/chat/messages (kèm trích dẫn, disclaimer), "
                    + "error {errorCode, message}.")
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@CurrentUser UserPrincipal me, @Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        Long userId = me.id();
        STREAM_EXECUTOR.execute(() -> {
            try {
                ChatResponse response = chatService.send(userId, request, new SseSink(emitter));
                emitter.send(SseEmitter.event().name("done").data(response, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (BusinessException e) {
                sendError(emitter, e.getErrorCode().name(), e.getMessage());
            } catch (Exception e) {
                log.error("Lỗi khi stream câu trả lời", e);
                sendError(emitter, ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getDefaultMessage());
            }
        });
        return emitter;
    }

    private static void sendError(SseEmitter emitter, String code, String message) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data(Map.of("errorCode", code, "message", message == null ? "" : message), MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (IOException | IllegalStateException ignored) {
            // Người dùng đã đóng kết nối
        }
    }

    /** Đẩy tiến trình lên trình duyệt; trình duyệt đóng kết nối giữa chừng thì bỏ qua, câu trả lời vẫn được lưu. */
    private record SseSink(SseEmitter emitter) implements ReplySink {

        @Override
        public void stage(Stage stage) {
            send("stage", Map.of("stage", stage.name()));
        }

        @Override
        public void delta(String text) {
            if (text != null && !text.isEmpty()) {
                send("delta", Map.of("text", text));
            }
        }

        private void send(String name, Object data) {
            try {
                emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ignored) {
                // Kết nối đã đóng
            }
        }
    }

    @Operation(summary = "UC-A11 bước 6 — Lưu các thẻ giao dịch đã xác nhận (BR-04)")
    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConfirmTransactionsResponse> confirm(@CurrentUser UserPrincipal me,
                                                            @Valid @RequestBody ConfirmTransactionsRequest request) {
        return ApiResponse.ok(chatService.confirmTransactions(me.id(), request));
    }

    @Operation(summary = "UC-B03 — Chuyển giọng nói thành văn bản ở server (tùy chọn)",
            description = "Phương án mặc định là Web Speech API ở trình duyệt. File âm thanh không được lưu (BR-09).")
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TranscriptionResponse> transcribe(@RequestPart("audio") MultipartFile audio) throws IOException {
        return ApiResponse.ok(chatService.transcribe(audio.getBytes(), audio.getOriginalFilename()));
    }

    @Operation(summary = "UC-A14 — Quét ảnh hóa đơn thành thẻ giao dịch nháp",
            description = "Ảnh JPG / PNG / WEBP tối đa 8 MB, không được lưu lại. Trả về như POST /api/chat/messages với "
                    + "payload.type = TRANSACTION_DRAFTS; lưu qua POST /api/chat/transactions với inputMode = IMAGE.")
    @PostMapping(value = "/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ChatResponse> receipt(@CurrentUser UserPrincipal me, @RequestPart("image") MultipartFile image,
                                             @RequestParam(required = false) Long conversationId) throws IOException {
        return ApiResponse.ok(chatService.receipt(me.id(), conversationId, image.getBytes(), image.getContentType()));
    }

    @Operation(summary = "UC-B09 — 👍 / 👎 cho câu trả lời của chatbot")
    @PostMapping("/messages/{id}/feedback")
    public ApiResponse<FeedbackResponses.FeedbackView> feedback(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                                                @Valid @RequestBody FeedbackRequest request) {
        return ApiResponse.ok(feedbackService.rate(me.id(), id, request), "Cảm ơn bạn đã góp ý");
    }
}
