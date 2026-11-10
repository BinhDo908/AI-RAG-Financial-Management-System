package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.dao.AiDataPermissionDao;
import com.fintech.chatbot.dto.request.AiDataPermissionRequest;
import com.fintech.chatbot.dto.response.AiDataPermissionResponse;
import com.fintech.chatbot.entity.AiDataPermission;
import com.fintech.chatbot.mapper.FinanceMapper;
import com.fintech.chatbot.service.AiDataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiDataPermissionServiceImpl implements AiDataPermissionService {

    private final AiDataPermissionDao permissionDao;
    private final FinanceMapper mapper;

    @Override
    @Transactional
    public AiDataPermissionResponse get(Long userId) {
        return mapper.toResponse(loadOrCreate(userId));
    }

    @Override
    @Transactional
    public AiDataPermissionResponse update(Long userId, AiDataPermissionRequest r) {
        AiDataPermission p = loadOrCreate(userId);
        p.setAllowTransactions(r.allowTransactions());
        p.setAllowBudgets(r.allowBudgets());
        p.setAllowDebts(r.allowDebts());
        p.setAllowGoals(r.allowGoals());
        p.setAllowProfile(r.allowProfile());
        return mapper.toResponse(p);
    }

    /** Tài khoản tạo trước khi có bản ghi quyền (ví dụ admin) được cấp mặc định bật tất cả. */
    private AiDataPermission loadOrCreate(Long userId) {
        return permissionDao.findByUserId(userId).orElseGet(() -> {
            AiDataPermission p = new AiDataPermission();
            p.setUserId(userId);
            return permissionDao.save(p);
        });
    }
}
