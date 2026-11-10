package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dao.NotificationDao;
import com.fintech.chatbot.dto.response.NotificationResponse;
import com.fintech.chatbot.entity.Notification;
import com.fintech.chatbot.mapper.FinanceMapper;
import com.fintech.chatbot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationDao notificationDao;
    private final FinanceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, boolean unreadOnly, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        var items = notificationDao.findByUser(userId, unreadOnly, safePage, safeSize).stream()
                .map(mapper::toResponse).toList();
        return PageResponse.of(items, safePage, safeSize, notificationDao.countByUser(userId, unreadOnly));
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationDao.countByUser(userId, true);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification n = notificationDao.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> BusinessException.notFound("thông báo"));
        n.setRead(true);
        return mapper.toResponse(n);
    }

    @Override
    @Transactional
    public int markAllRead(Long userId) {
        return notificationDao.markAllRead(userId);
    }
}
