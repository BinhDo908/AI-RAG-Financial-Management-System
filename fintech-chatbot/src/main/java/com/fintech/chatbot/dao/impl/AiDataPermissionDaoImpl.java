package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.AiDataPermissionDao;
import com.fintech.chatbot.entity.AiDataPermission;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AiDataPermissionDaoImpl extends AbstractUserOwnedDao<AiDataPermission> implements AiDataPermissionDao {

    public AiDataPermissionDaoImpl() {
        super(AiDataPermission.class);
    }

    @Override
    public Optional<AiDataPermission> findByUserId(Long userId) {
        return query("from AiDataPermission e where e.userId = :userId")
                .setParameter("userId", userId)
                .uniqueResultOptional();
    }
}
