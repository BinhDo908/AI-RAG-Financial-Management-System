package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.MarketRate;
import com.fintech.chatbot.entity.enums.MarketRateKind;

import java.util.Optional;

/**
 * DAO cho E-18 MarketRate (UC-B07).
 */
public interface MarketRateDao extends GenericDao<MarketRate, Long> {

    /** Bản ghi mới nhất của một mã, dùng làm cache (làm mới sau 60 phút). */
    Optional<MarketRate> findLatest(MarketRateKind kind, String symbol);
}
