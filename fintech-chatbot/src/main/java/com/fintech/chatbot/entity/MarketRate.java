package com.fintech.chatbot.entity;

import com.fintech.chatbot.entity.enums.MarketRateKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * E-18 — Bản ghi cache dữ liệu thị trường (UC-B07). Luôn hiển thị kèm nguồn và thời điểm (BR-12).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "market_rates")
public class MarketRate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private MarketRateKind kind;

    /** Ví dụ "USD", "SJC_1L", "SAVING_12M". */
    @Column(name = "symbol", nullable = false, length = 30)
    private String symbol;

    @Column(name = "buy", precision = 18, scale = 4)
    private BigDecimal buy;

    @Column(name = "sell", precision = 18, scale = 4)
    private BigDecimal sell;

    @Column(name = "source_id", nullable = false, length = 20)
    private String sourceId;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}
