package com.fintech.chatbot.entity;

import com.fintech.chatbot.entity.enums.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * E-09 — Hồ sơ tài chính (UC-B04). Mỗi user có tối đa 1 hồ sơ.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "financial_profiles")
public class FinancialProfile extends UserOwnedEntity {

    @Column(name = "monthly_income", precision = 18, scale = 0)
    private BigDecimal monthlyIncome;

    @Column(name = "dependents", nullable = false)
    private int dependents;

    /** Danh sách mục tiêu dạng mã, ngăn cách bằng dấu phẩy, ví dụ "EMERGENCY_FUND,PAY_DEBT". */
    @Column(name = "goals", length = 500)
    private String goals;

    /** Tổng điểm bảng hỏi rủi ro 5 câu (5–15). */
    @Column(name = "risk_score")
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 10)
    private RiskLevel riskLevel;
}
