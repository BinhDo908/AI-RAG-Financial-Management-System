package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.EvalRunDao;
import com.fintech.chatbot.entity.EvalRun;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EvalRunDaoImpl extends AbstractHibernateDao<EvalRun> implements EvalRunDao {

    public EvalRunDaoImpl() {
        super(EvalRun.class);
    }

    @Override
    public List<EvalRun> findRecent(int limit) {
        return query("from EvalRun r order by r.createdAt desc, r.id desc")
                .setMaxResults(limit)
                .getResultList();
    }
}
