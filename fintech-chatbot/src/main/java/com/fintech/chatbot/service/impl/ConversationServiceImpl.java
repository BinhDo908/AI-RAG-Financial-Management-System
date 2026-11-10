package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dao.ConversationDao;
import com.fintech.chatbot.dao.MessageDao;
import com.fintech.chatbot.dto.response.ConversationDetailResponse;
import com.fintech.chatbot.dto.response.ConversationResponse;
import com.fintech.chatbot.entity.Conversation;
import com.fintech.chatbot.mapper.ChatMessageViews;
import com.fintech.chatbot.mapper.FinanceMapper;
import com.fintech.chatbot.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    public static final String DEFAULT_TITLE = "Hội thoại mới";
    private static final int MAX_PAGE_SIZE = 100;

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final FinanceMapper mapper;
    private final ChatMessageViews messageViews;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> list(Long userId, String keyword, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        var items = conversationDao.search(userId, keyword, safePage, safeSize).stream()
                .map(mapper::toResponse).toList();
        return PageResponse.of(items, safePage, safeSize, conversationDao.countSearch(userId, keyword));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponse get(Long userId, Long conversationId) {
        Conversation c = load(userId, conversationId);
        var messages = messageDao.findByConversationId(c.getId()).stream().map(messageViews::toView).toList();
        return new ConversationDetailResponse(c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt(), messages);
    }

    @Override
    @Transactional
    public ConversationResponse create(Long userId, String title) {
        Conversation c = new Conversation();
        c.setUserId(userId);
        c.setTitle(title == null || title.isBlank() ? DEFAULT_TITLE : title.trim());
        conversationDao.save(c);
        return mapper.toResponse(c);
    }

    @Override
    @Transactional
    public ConversationResponse rename(Long userId, Long conversationId, String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tiêu đề không được để trống");
        }
        Conversation c = load(userId, conversationId);
        c.setTitle(title.trim());
        return mapper.toResponse(c);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long conversationId) {
        conversationDao.delete(load(userId, conversationId));
    }

    private Conversation load(Long userId, Long conversationId) {
        return conversationDao.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
    }
}
