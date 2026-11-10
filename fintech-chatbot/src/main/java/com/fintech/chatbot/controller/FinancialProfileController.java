package com.fintech.chatbot.controller;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.FinancialProfileRequest;
import com.fintech.chatbot.dto.request.RiskAnswersRequest;
import com.fintech.chatbot.dto.response.FinancialProfileResponse;
import com.fintech.chatbot.dto.response.RiskQuestionResponse;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.FinancialProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Financial profile", description = "UC-B04 Hồ sơ tài chính")
@RestController
@RequestMapping("/api/financial-profile")
@RequiredArgsConstructor
public class FinancialProfileController {

    private final FinancialProfileService profileService;

    @Operation(summary = "Xem hồ sơ tài chính")
    @GetMapping
    public ApiResponse<FinancialProfileResponse> get(@CurrentUser UserPrincipal me) {
        return ApiResponse.ok(profileService.get(me.id()));
    }

    @Operation(summary = "Cập nhật thu nhập, người phụ thuộc, mục tiêu")
    @PutMapping
    public ApiResponse<FinancialProfileResponse> update(@CurrentUser UserPrincipal me,
                                                        @Valid @RequestBody FinancialProfileRequest request) {
        return ApiResponse.ok(profileService.update(me.id(), request), "Đã lưu hồ sơ");
    }

    @Operation(summary = "Bảng hỏi khẩu vị rủi ro (5 câu)")
    @GetMapping("/risk-questions")
    public ApiResponse<List<RiskQuestionResponse>> riskQuestions() {
        return ApiResponse.ok(profileService.riskQuestions());
    }

    @Operation(summary = "Nộp câu trả lời, tính mức rủi ro")
    @PostMapping("/risk-assessment")
    public ApiResponse<FinancialProfileResponse> assess(@CurrentUser UserPrincipal me,
                                                        @Valid @RequestBody RiskAnswersRequest request) {
        return ApiResponse.ok(profileService.submitRiskAnswers(me.id(), request));
    }
}
