package com.fintech.chatbot.ai.market;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.dao.MarketRateDao;
import com.fintech.chatbot.entity.MarketRate;
import com.fintech.chatbot.entity.enums.MarketRateKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * UC-B07 bước 2 — Tỷ giá có cache 60 phút trong bảng market_rates (E-18).
 * Nguồn lỗi → dùng cache gần nhất kèm thời điểm (EX-1).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketRateService {

    static final long CACHE_MINUTES = 60;

    private final MarketRateDao marketRateDao;
    private final ExchangeRateProvider provider;
    private final Clock clock;

    /** @param stale true khi nguồn lỗi và đang dùng dữ liệu cache cũ */
    public record FxRate(String base, String quote, BigDecimal rate, LocalDateTime fetchedAt, String sourceId,
                         String sourceName, boolean stale) {
    }

    /** Gọi trong transaction của service. */
    public FxRate fx(String base, String quote) {
        String symbol = base + "/" + quote;
        Optional<MarketRate> cached = marketRateDao.findLatest(MarketRateKind.FX, symbol);
        LocalDateTime now = LocalDateTime.now(clock);
        if (cached.isPresent() && cached.get().getFetchedAt().isAfter(now.minusMinutes(CACHE_MINUTES))) {
            return toFx(base, quote, cached.get(), false);
        }
        try {
            ExchangeRateProvider.Quote q = provider.fetch(base, quote);
            MarketRate rate = new MarketRate();
            rate.setKind(MarketRateKind.FX);
            rate.setSymbol(symbol);
            rate.setBuy(q.rate());
            rate.setSell(q.rate());
            rate.setSourceId(provider.sourceId());
            rate.setFetchedAt(now);
            marketRateDao.save(rate);
            return new FxRate(base, quote, q.rate(), q.updatedAt(), provider.sourceId(), provider.sourceName(), false);
        } catch (BusinessException e) {
            if (cached.isPresent()) {
                log.warn("Nguồn tỷ giá lỗi, dùng cache {}", symbol);
                return toFx(base, quote, cached.get(), true);
            }
            throw e;
        }
    }

    private FxRate toFx(String base, String quote, MarketRate r, boolean stale) {
        return new FxRate(base, quote, r.getSell(), r.getFetchedAt(), r.getSourceId(), provider.sourceName(), stale);
    }
}
