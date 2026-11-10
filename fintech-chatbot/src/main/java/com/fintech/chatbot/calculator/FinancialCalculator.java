package com.fintech.chatbot.calculator;

import com.fintech.chatbot.dto.response.CalculatorResponses.CompoundInterest;
import com.fintech.chatbot.dto.response.CalculatorResponses.Loan;
import com.fintech.chatbot.dto.response.CalculatorResponses.LoanComparison;
import com.fintech.chatbot.dto.response.CalculatorResponses.LoanRow;
import com.fintech.chatbot.dto.response.CalculatorResponses.Method;
import com.fintech.chatbot.dto.response.CalculatorResponses.SavingTarget;
import com.fintech.chatbot.dto.response.CalculatorResponses.YearRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * UC-B05 — Máy tính tài chính, công thức theo USE_CASES.md (BR-06: số liệu do code tính).
 *
 * <p>Ký hiệu: i = lãi suất mỗi kỳ = lãi suất năm / số kỳ trong năm; n = số kỳ.
 * Tính trung gian với độ chính xác 34 chữ số, chỉ làm tròn đến đồng khi trả kết quả.
 * Với lịch trả nợ, lãi mỗi kỳ được làm tròn đến đồng và kỳ cuối tự điều chỉnh để dư nợ về đúng 0.
 */
@Component
public class FinancialCalculator {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MONTHS_PER_YEAR = 12;

    /**
     * (1) Lãi kép có góp định kỳ cuối mỗi kỳ:
     * FV = P·(1+i)ⁿ + PMT·[((1+i)ⁿ − 1)/i]; nếu i = 0 thì FV = P + PMT·n.
     */
    public CompoundInterest compoundInterest(BigDecimal principal, BigDecimal contribution,
                                             BigDecimal annualRatePercent, int periodsPerYear, int years) {
        BigDecimal i = periodRate(annualRatePercent, periodsPerYear);
        List<YearRow> schedule = new ArrayList<>();
        BigDecimal balance = principal;
        BigDecimal contributed = principal;
        for (int year = 1; year <= years; year++) {
            int n = year * periodsPerYear;
            balance = futureValue(principal, contribution, i, n);
            contributed = principal.add(contribution.multiply(BigDecimal.valueOf(n)));
            schedule.add(new YearRow(year, vnd(contributed), vnd(balance.subtract(contributed)), vnd(balance)));
        }
        return new CompoundInterest(vnd(balance), vnd(contributed), vnd(balance.subtract(contributed)), schedule);
    }

    /** (2) Dư nợ giảm dần, trả đều: PMT = P·i / (1 − (1+i)⁻ⁿ); nếu i = 0 thì PMT = P/n. */
    public Loan annuityLoan(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        BigDecimal i = periodRate(annualRatePercent, MONTHS_PER_YEAR);
        BigDecimal payment;
        if (i.signum() == 0) {
            payment = principal.divide(BigDecimal.valueOf(months), MC);
        } else {
            BigDecimal discount = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(onePlus(i).pow(months, MC), MC));
            payment = principal.multiply(i, MC).divide(discount, MC);
        }
        BigDecimal roundedPayment = vnd(payment);

        List<LoanRow> rows = new ArrayList<>();
        BigDecimal remaining = principal;
        for (int k = 1; k <= months; k++) {
            BigDecimal interest = vnd(remaining.multiply(i, MC));
            BigDecimal principalPart = k == months ? remaining : roundedPayment.subtract(interest).min(remaining);
            remaining = remaining.subtract(principalPart);
            rows.add(new LoanRow(k, principalPart.add(interest), principalPart, interest, remaining));
        }
        return toLoan(Method.ANNUITY, rows);
    }

    /** (3) Gốc trả đều: gốc mỗi kỳ = P/n; lãi kỳ k = dư nợ đầu kỳ k × i. */
    public Loan equalPrincipalLoan(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        BigDecimal i = periodRate(annualRatePercent, MONTHS_PER_YEAR);
        BigDecimal principalPart = vnd(principal.divide(BigDecimal.valueOf(months), MC));
        List<LoanRow> rows = new ArrayList<>();
        BigDecimal remaining = principal;
        for (int k = 1; k <= months; k++) {
            BigDecimal interest = vnd(remaining.multiply(i, MC));
            BigDecimal p = k == months ? remaining : principalPart.min(remaining);
            remaining = remaining.subtract(p);
            rows.add(new LoanRow(k, p.add(interest), p, interest, remaining));
        }
        return toLoan(Method.EQUAL_PRINCIPAL, rows);
    }

    /** (4) Lãi phẳng: lãi mỗi kỳ = P × i, tính trên gốc ban đầu suốt kỳ hạn; gốc trả đều P/n. */
    public Loan flatRateLoan(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        BigDecimal i = periodRate(annualRatePercent, MONTHS_PER_YEAR);
        BigDecimal interest = vnd(principal.multiply(i, MC));
        BigDecimal principalPart = vnd(principal.divide(BigDecimal.valueOf(months), MC));
        List<LoanRow> rows = new ArrayList<>();
        BigDecimal remaining = principal;
        for (int k = 1; k <= months; k++) {
            BigDecimal p = k == months ? remaining : principalPart.min(remaining);
            remaining = remaining.subtract(p);
            rows.add(new LoanRow(k, p.add(interest), p, interest, remaining));
        }
        return toLoan(Method.FLAT, rows);
    }

    /** AF-1: cùng lãi suất danh nghĩa, lãi phẳng tốn thêm bao nhiêu so với dư nợ giảm dần trả đều. */
    public LoanComparison compareFlatVsAnnuity(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        Loan flat = flatRateLoan(principal, annualRatePercent, months);
        Loan annuity = annuityLoan(principal, annualRatePercent, months);
        return new LoanComparison(flat, annuity, flat.totalInterest().subtract(annuity.totalInterest()));
    }

    /**
     * (5) Góp mỗi tháng để đạt mục tiêu FV khi đã có PV:
     * PMT = (FV − PV·(1+i)ⁿ)·i / ((1+i)ⁿ − 1); nếu i = 0 thì PMT = (FV − PV)/n. Làm tròn lên đến đồng.
     */
    public SavingTarget savingTarget(BigDecimal target, BigDecimal current, BigDecimal annualRatePercent, int months) {
        BigDecimal pv = current == null ? BigDecimal.ZERO : current;
        BigDecimal i = periodRate(annualRatePercent, MONTHS_PER_YEAR);
        BigDecimal pvGrown = pv.multiply(onePlus(i).pow(months, MC), MC);
        BigDecimal gap = target.subtract(pvGrown);

        BigDecimal payment;
        if (gap.signum() <= 0) {
            payment = BigDecimal.ZERO;
        } else if (i.signum() == 0) {
            payment = gap.divide(BigDecimal.valueOf(months), MC);
        } else {
            payment = gap.multiply(i, MC).divide(onePlus(i).pow(months, MC).subtract(BigDecimal.ONE), MC);
        }
        payment = payment.setScale(0, RoundingMode.CEILING);

        BigDecimal projected = futureValue(pv, payment, i, months);
        BigDecimal contributed = pv.add(payment.multiply(BigDecimal.valueOf(months)));
        return new SavingTarget(payment, vnd(contributed), vnd(projected.subtract(contributed)), vnd(projected));
    }

    // ---------- tiện ích ----------

    private static BigDecimal futureValue(BigDecimal principal, BigDecimal contribution, BigDecimal i, int n) {
        if (i.signum() == 0) {
            return principal.add(contribution.multiply(BigDecimal.valueOf(n)));
        }
        BigDecimal growth = onePlus(i).pow(n, MC);
        BigDecimal annuity = growth.subtract(BigDecimal.ONE).divide(i, MC);
        return principal.multiply(growth, MC).add(contribution.multiply(annuity, MC));
    }

    private static BigDecimal periodRate(BigDecimal annualRatePercent, int periodsPerYear) {
        return annualRatePercent.divide(HUNDRED, MC).divide(BigDecimal.valueOf(periodsPerYear), MC);
    }

    private static BigDecimal onePlus(BigDecimal i) {
        return BigDecimal.ONE.add(i);
    }

    private static BigDecimal vnd(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private static Loan toLoan(Method method, List<LoanRow> rows) {
        BigDecimal totalPayment = rows.stream().map(LoanRow::payment).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInterest = rows.stream().map(LoanRow::interest).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Loan(method, rows.get(0).payment(), rows.get(rows.size() - 1).payment(),
                totalPayment, totalInterest, rows);
    }
}
