package com.fintech.chatbot.controller.admin;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.request.EvaluationRequest;
import com.fintech.chatbot.dto.response.ChatQualityResponses;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.ChatQualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Admin - Chat quality", description = "UC-C03 Dashboard chất lượng chatbot")
@RestController
@RequestMapping("/api/admin/chat-quality")
@RequiredArgsConstructor
public class AdminChatQualityController {

    private final ChatQualityService qualityService;

    @Operation(summary = "Dashboard (mặc định 30 ngày gần nhất)")
    @GetMapping
    public ApiResponse<ChatQualityResponses.Dashboard> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.ok(qualityService.dashboard(from, to));
    }

    @Operation(summary = "AF-1 — Chạy bộ đánh giá (tối đa 100 câu / lần)")
    @PostMapping("/evaluations")
    public ApiResponse<ChatQualityResponses.EvalRunDetail> evaluate(@CurrentUser UserPrincipal admin,
                                                                    @Valid @RequestBody EvaluationRequest request) {
        return ApiResponse.ok(qualityService.evaluate(admin.id(), request));
    }

    @Operation(summary = "Lịch sử các lần đánh giá")
    @GetMapping("/evaluations")
    public ApiResponse<List<ChatQualityResponses.EvalRunView>> evaluations() {
        return ApiResponse.ok(qualityService.evaluations());
    }

    @Operation(summary = "Chi tiết một lần đánh giá")
    @GetMapping("/evaluations/{id}")
    public ApiResponse<ChatQualityResponses.EvalRunDetail> evaluation(@PathVariable Long id) {
        return ApiResponse.ok(qualityService.evaluation(id));
    }
}
