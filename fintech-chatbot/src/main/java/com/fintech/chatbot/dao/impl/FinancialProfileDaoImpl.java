package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.FinancialProfileDao;
import com.fintech.chatbot.entity.FinancialProfile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class FinancialProfileDaoImpl extends AbstractUserOwnedDao<FinancialProfile> implements FinancialProfileDao {

    public FinancialProfileDaoImpl() {
        super(FinancialProfile.class);
    }

    @Override
    public Optional<FinancialProfile> findByUserId(Long userId) {
        return query("from FinancialProfile e where e.userId = :userId")
                .setParameter("userId", userId)
                .uniqueResultOptional();
    }
}
