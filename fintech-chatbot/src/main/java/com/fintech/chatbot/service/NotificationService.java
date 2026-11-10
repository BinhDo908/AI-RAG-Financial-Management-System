package com.fintech.chatbot.service;

import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dto.response.NotificationResponse;

/**
 * Chuông thông báo trong ứng dụng (UC-B06, UC-D04).
 */
public interface NotificationService {

    PageResponse<NotificationResponse> list(Long userId, boolean unreadOnly, int page, int size);

    long unreadCount(Long userId);

    NotificationResponse markRead(Long userId, Long notificationId);

    int markAllRead(Long userId);
}
