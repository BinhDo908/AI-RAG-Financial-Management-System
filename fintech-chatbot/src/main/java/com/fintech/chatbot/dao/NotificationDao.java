package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Notification;
import com.fintech.chatbot.entity.enums.NotificationType;

import java.util.List;

/**
 * DAO cho E-15 Notification (UC-B06, D04).
 */
public interface NotificationDao extends UserOwnedDao<Notification, Long> {

    List<Notification> findByUser(Long userId, boolean unreadOnly, int page, int size);

    long countByUser(Long userId, boolean unreadOnly);

    /** Dùng để chỉ gửi mỗi loại cảnh báo một lần cho cùng đối tượng (UC-B06 EX-1). */
    boolean exists(Long userId, NotificationType type, Long refId);

    int markAllRead(Long userId);
}
