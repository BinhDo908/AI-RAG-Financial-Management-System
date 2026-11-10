package com.fintech.chatbot.calculator;

import com.fintech.chatbot.dto.response.CalculatorResponses.Loan;
import com.fintech.chatbot.dto.response.CalculatorResponses.LoanRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra công thức UC-B05 bằng các giá trị tính tay.
 */
class FinancialCalculatorTest {

    private final FinancialCalculator calc = new FinancialCalculator();

    private static BigDecimal vnd(long value) {
        return BigDecimal.valueOf(value);
    }

    @Test
    void compoundInterestYearly() {
        // 10.000.000 × 1,12² = 12.544.000
        var r = calc.compoundInterest(vnd(10_000_000), BigDecimal.ZERO, BigDecimal.valueOf(12), 1, 2);
        assertThat(r.futureValue()).isEqualByComparingTo("12544000");
        assertThat(r.totalInterest()).isEqualByComparingTo("2544000");
        assertThat(r.schedule()).hasSize(2);
        assertThat(r.schedule().get(0).balance()).isEqualByComparingTo("11200000");
    }

    @Test
    void compoundInterestWithMonthlyContribution() {
        // 1.000.000 × ((1,01¹² − 1) / 0,01) = 12.682.503,01
        var r = calc.compoundInterest(BigDecimal.ZERO, vnd(1_000_000), BigDecimal.valueOf(12), 12, 1);
        assertThat(r.futureValue()).isEqualByComparingTo("12682503");
        assertThat(r.totalContributed()).isEqualByComparingTo("12000000");
    }

    @Test
    void compoundInterestZeroRate() {
        var r = calc.compoundInterest(vnd(1_000), vnd(100), BigDecimal.ZERO, 12, 1);
        assertThat(r.futureValue()).isEqualByComparingTo("2200");
        assertThat(r.totalInterest()).isEqualByComparingTo("0");
    }

    @Test
    void annuityLoanMatchesFormulaAndClearsBalance() {
        // PMT = 100.000.000 × 0,01 / (1 − 1,01⁻¹²) = 8.884.878,79
        Loan loan = calc.annuityLoan(vnd(100_000_000), BigDecimal.valueOf(12), 12);
        assertThat(loan.firstPayment()).isEqualByComparingTo("8884879");
        assertThat(loan.schedule()).hasSize(12);
        assertThat(loan.schedule().get(0).interest()).isEqualByComparingTo("1000000");
        assertThat(loan.schedule().get(11).remaining()).isEqualByComparingTo("0");
        assertThat(sumPrincipal(loan)).isEqualByComparingTo("100000000");
        assertThat(loan.totalPayment()).isEqualByComparingTo(loan.totalInterest().add(vnd(100_000_000)));
        // Kỳ cuối điều chỉnh làm tròn nên chỉ lệch vài đồng
        assertThat(loan.lastPayment().subtract(loan.firstPayment()).abs()).isLessThan(vnd(20));
    }

    @Test
    void equalPrincipalLoan() {
        // Gốc 1.000.000 / tháng; lãi = 1% × dư nợ: 120.000, 110.000, …, 10.000 → tổng 780.000
        Loan loan = calc.equalPrincipalLoan(vnd(12_000_000), BigDecimal.valueOf(12), 12);
        assertThat(loan.firstPayment()).isEqualByComparingTo("1120000");
        assertThat(loan.lastPayment()).isEqualByComparingTo("1010000");
        assertThat(loan.totalInterest()).isEqualByComparingTo("780000");
    }

    @Test
    void flatRateCostsMoreThanAnnuity() {
        var cmp = calc.compareFlatVsAnnuity(vnd(12_000_000), BigDecimal.valueOf(12), 12);
        // Lãi phẳng: 120.000 × 12 = 1.440.000
        assertThat(cmp.flat().totalInterest()).isEqualByComparingTo("1440000");
        assertThat(cmp.flat().firstPayment()).isEqualByComparingTo("1120000");
        assertThat(cmp.extraInterestOfFlat()).isPositive();
        assertThat(cmp.extraInterestOfFlat())
                .isEqualByComparingTo(cmp.flat().totalInterest().subtract(cmp.annuity().totalInterest()));
    }

    @Test
    void savingTarget() {
        // Lãi 0: 12.000.000 / 12 = 1.000.000
        var zero = calc.savingTarget(vnd(12_000_000), BigDecimal.ZERO, BigDecimal.ZERO, 12);
        assertThat(zero.monthlyContribution()).isEqualByComparingTo("1000000");

        // 12%/năm: 120.000 / (1,01¹² − 1) = 946.185,4 → làm tròn lên 946.186
        var withRate = calc.savingTarget(vnd(12_000_000), BigDecimal.ZERO, BigDecimal.valueOf(12), 12);
        assertThat(withRate.monthlyContribution()).isEqualByComparingTo("946186");
        assertThat(withRate.projectedValue()).isGreaterThanOrEqualTo(vnd(12_000_000));

        // Số tiền hiện có đã đủ → không cần góp thêm
        var done = calc.savingTarget(vnd(1_000_000), vnd(2_000_000), BigDecimal.valueOf(5), 12);
        assertThat(done.monthlyContribution()).isEqualByComparingTo("0");
    }

    private static BigDecimal sumPrincipal(Loan loan) {
        return loan.schedule().stream().map(LoanRow::principal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
