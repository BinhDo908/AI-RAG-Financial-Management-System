package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Debt;
import com.fintech.chatbot.entity.enums.DebtStatus;
import com.fintech.chatbot.entity.enums.DebtType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * DAO cho E-07 Debt (UC-A06, A09, D04).
 */
public interface DebtDao extends UserOwnedDao<Debt, Long> {

    List<Debt> findByUser(Long userId, DebtType type, DebtStatus status);

    /** Tổng dư nợ còn lại theo loại (UC-A06 AF-1, UC-A09 tài sản ròng). */
    BigDecimal sumRemaining(Long userId, DebtType type);

    /** Khoản nợ chưa trả có hạn rơi đúng vào một trong các ngày (mọi user) — UC-D04. */
    List<Debt> findOpenDueOn(Collection<LocalDate> dates);
}
