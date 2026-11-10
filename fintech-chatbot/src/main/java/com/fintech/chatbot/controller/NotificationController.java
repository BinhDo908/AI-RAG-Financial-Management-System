package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dto.response.NotificationResponse;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications", description = "UC-B06 Cảnh báo ngân sách, UC-D04 Nhắc nhở tự động")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Danh sách thông báo, mới nhất trước")
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(@CurrentUser UserPrincipal me,
                                                                @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(notificationService.list(me.id(), unreadOnly, page, size));
    }

    @Operation(summary = "Số thông báo chưa đọc (hiển thị trên chuông)")
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@CurrentUser UserPrincipal me) {
        return ApiResponse.ok(notificationService.unreadCount(me.id()));
    }

    @Operation(summary = "Đánh dấu đã đọc")
    @PostMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@CurrentUser UserPrincipal me, @PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(me.id(), id));
    }

    @Operation(summary = "Đánh dấu tất cả đã đọc")
    @PostMapping("/read-all")
    public ApiResponse<Integer> markAllRead(@CurrentUser UserPrincipal me) {
        return ApiResponse.ok(notificationService.markAllRead(me.id()));
    }
}
