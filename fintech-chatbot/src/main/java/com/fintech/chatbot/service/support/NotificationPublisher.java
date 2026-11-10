package com.fintech.chatbot.service.support;

import com.fintech.chatbot.dao.NotificationDao;
import com.fintech.chatbot.entity.Notification;
import com.fintech.chatbot.entity.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Tạo thông báo trong ứng dụng (E-15). Giai đoạn 3 sẽ đẩy thêm vào khung chat.
 */
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final int MAX_CONTENT = 500;

    private final NotificationDao notificationDao;

    public Notification publish(Long userId, NotificationType type, String content, Long refId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setContent(content.length() > MAX_CONTENT ? content.substring(0, MAX_CONTENT) : content);
        n.setRefId(refId);
        return notificationDao.save(n);
    }

    /** Chỉ tạo nếu chưa từng có thông báo cùng loại cho cùng đối tượng. */
    public boolean publishOnce(Long userId, NotificationType type, String content, Long refId) {
        if (notificationDao.exists(userId, type, refId)) {
            return false;
        }
        publish(userId, type, content, refId);
        return true;
    }
}
