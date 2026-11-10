package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.SavingGoalDao;
import com.fintech.chatbot.entity.SavingGoal;
import org.springframework.stereotype.Repository;

@Repository
public class SavingGoalDaoImpl extends AbstractUserOwnedDao<SavingGoal> implements SavingGoalDao {

    public SavingGoalDaoImpl() {
        super(SavingGoal.class);
    }
}
