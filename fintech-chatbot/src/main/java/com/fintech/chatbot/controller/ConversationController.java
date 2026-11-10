package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dto.request.ConversationRequest;
import com.fintech.chatbot.dto.response.ConversationDetailResponse;
import com.fintech.chatbot.dto.response.ConversationResponse;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Conversations", description = "UC-B08 Lịch sử hội thoại")
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Operation(summary = "Danh sách hội thoại, mới nhất trước; tìm theo tiêu đề")
    @GetMapping
    public ApiResponse<PageResponse<ConversationResponse>> list(@CurrentUser UserPrincipal me,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(conversationService.list(me.id(), keyword, page, size));
    }

    @Operation(summary = "Mở hội thoại kèm toàn bộ tin nhắn")
    @GetMapping("/{id}")
    public ApiResponse<ConversationDetailResponse> get(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(conversationService.get(me.id(), id));
    }

    @Operation(summary = "Hội thoại mới")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> create(@CurrentUser UserPrincipal me,
                                                    @Valid @RequestBody(required = false) ConversationRequest request) {
        return ApiResponse.ok(conversationService.create(me.id(), request == null ? null : request.title()));
    }

    @Operation(summary = "Đổi tên hội thoại")
    @PatchMapping("/{id}")
    public ApiResponse<ConversationResponse> rename(@CurrentUser UserPrincipal me, @PathVariable Long id,
                                                    @Valid @RequestBody ConversationRequest request) {
        return ApiResponse.ok(conversationService.rename(me.id(), id, request.title()));
    }

    @Operation(summary = "Xóa hội thoại (giao dịch đã lưu từ hội thoại được giữ lại)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        conversationService.delete(me.id(), id);
        return ApiResponse.message("Đã xóa hội thoại");
    }
}
