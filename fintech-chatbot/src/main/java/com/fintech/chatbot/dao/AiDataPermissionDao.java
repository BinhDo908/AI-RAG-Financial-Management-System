package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.AiDataPermission;

import java.util.Optional;

/**
 * DAO cho E-17 AiDataPermission (UC-D05). Mỗi user có tối đa một bản ghi.
 */
public interface AiDataPermissionDao extends UserOwnedDao<AiDataPermission, Long> {

    Optional<AiDataPermission> findByUserId(Long userId);
}
