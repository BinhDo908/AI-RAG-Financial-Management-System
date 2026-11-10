package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.RecurringRule;

import java.time.LocalDate;
import java.util.List;

/**
 * DAO cho E-08 RecurringRule (UC-A07, D04).
 */
public interface RecurringRuleDao extends UserOwnedDao<RecurringRule, Long> {

    /** Id các quy tắc đang bật đã đến hạn chạy (mọi user) — dùng bởi job nền UC-D04. */
    List<Long> findDueIds(LocalDate today);

    boolean existsByWallet(Long walletId);

    int reassignCategory(Long userId, Long fromCategoryId, Long toCategoryId);
}
