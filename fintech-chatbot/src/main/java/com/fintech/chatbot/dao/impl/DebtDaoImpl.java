package com.fintech.chatbot.dao.impl;

import com.fintech.chatbot.dao.DebtDao;
import com.fintech.chatbot.entity.Debt;
import com.fintech.chatbot.entity.enums.DebtStatus;
import com.fintech.chatbot.entity.enums.DebtType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public class DebtDaoImpl extends AbstractUserOwnedDao<Debt> implements DebtDao {

    public DebtDaoImpl() {
        super(Debt.class);
    }

    @Override
    public List<Debt> findByUser(Long userId, DebtType type, DebtStatus status) {
        return query("""
                from Debt d
                where d.userId = :userId
                  and (:type is null or d.type = :type)
                  and (:status is null or d.status = :status)
                order by d.status, d.dueDate nulls last, d.id desc
                """)
                .setParameter("userId", userId)
                .setParameter("type", type)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public BigDecimal sumRemaining(Long userId, DebtType type) {
        BigDecimal result = session().createSelectionQuery("""
                        select sum(d.remaining) from Debt d
                        where d.userId = :userId and d.type = :type and d.status = :open
                        """, BigDecimal.class)
                .setParameter("userId", userId)
                .setParameter("type", type)
                .setParameter("open", DebtStatus.OPEN)
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }

    @Override
    public List<Debt> findOpenDueOn(Collection<LocalDate> dates) {
        return query("from Debt d where d.status = :open and d.dueDate in (:dates)")
                .setParameter("open", DebtStatus.OPEN)
                .setParameterList("dates", dates)
                .getResultList();
    }
}
