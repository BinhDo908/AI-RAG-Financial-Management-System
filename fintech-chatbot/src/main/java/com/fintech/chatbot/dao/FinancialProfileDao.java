package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.FinancialProfile;

import java.util.Optional;

/**
 * DAO cho E-09 FinancialProfile (UC-B04). Mỗi user có tối đa một bản ghi.
 */
public interface FinancialProfileDao extends UserOwnedDao<FinancialProfile, Long> {

    Optional<FinancialProfile> findByUserId(Long userId);
}
