package com.fintech.chatbot.controller;

import com.fintech.chatbot.calculator.FinancialCalculator;
import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.CompoundInterestRequest;
import com.fintech.chatbot.dto.request.LoanRequest;
import com.fintech.chatbot.dto.request.SavingTargetRequest;
import com.fintech.chatbot.dto.response.CalculatorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Công cụ không đọc / ghi dữ liệu nên gọi thẳng FinancialCalculator, không qua tầng service + DAO.
 */
@Tag(name = "Tools", description = "UC-B05 Công cụ mô phỏng tài chính")
@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class CalculatorController {

    private final FinancialCalculator calculator;

    @Operation(summary = "(1) Lãi kép có góp định kỳ")
    @PostMapping("/compound-interest")
    public ApiResponse<CalculatorResponses.CompoundInterest> compound(@Valid @RequestBody CompoundInterestRequest r) {
        return ApiResponse.ok(calculator.compoundInterest(r.principal(), r.contribution(), r.annualRatePercent(),
                r.periodsPerYear(), r.years()));
    }

    @Operation(summary = "(2) Trả góp dư nợ giảm dần, trả đều mỗi tháng")
    @PostMapping("/loan/annuity")
    public ApiResponse<CalculatorResponses.Loan> annuity(@Valid @RequestBody LoanRequest r) {
        return ApiResponse.ok(calculator.annuityLoan(r.principal(), r.annualRatePercent(), r.months()));
    }

    @Operation(summary = "(3) Trả góp gốc đều")
    @PostMapping("/loan/equal-principal")
    public ApiResponse<CalculatorResponses.Loan> equalPrincipal(@Valid @RequestBody LoanRequest r) {
        return ApiResponse.ok(calculator.equalPrincipalLoan(r.principal(), r.annualRatePercent(), r.months()));
    }

    @Operation(summary = "(4) Lãi phẳng")
    @PostMapping("/loan/flat")
    public ApiResponse<CalculatorResponses.Loan> flat(@Valid @RequestBody LoanRequest r) {
        return ApiResponse.ok(calculator.flatRateLoan(r.principal(), r.annualRatePercent(), r.months()));
    }

    @Operation(summary = "AF-1 — So sánh lãi phẳng với dư nợ giảm dần")
    @PostMapping("/loan/compare")
    public ApiResponse<CalculatorResponses.LoanComparison> compare(@Valid @RequestBody LoanRequest r) {
        return ApiResponse.ok(calculator.compareFlatVsAnnuity(r.principal(), r.annualRatePercent(), r.months()));
    }

    @Operation(summary = "(5) Tiết kiệm đạt mục tiêu: cần góp bao nhiêu mỗi tháng")
    @PostMapping("/saving-target")
    public ApiResponse<CalculatorResponses.SavingTarget> savingTarget(@Valid @RequestBody SavingTargetRequest r) {
        return ApiResponse.ok(calculator.savingTarget(r.targetAmount(), r.currentAmount(), r.annualRatePercent(),
                r.months()));
    }
}
