package com.fintech.chatbot.controller.admin;

import com.fintech.chatbot.common.response.ApiResponse;
import com.fintech.chatbot.dto.response.AiConfigResponse;
import com.fintech.chatbot.security.CurrentUser;
import com.fintech.chatbot.security.UserPrincipal;
import com.fintech.chatbot.service.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Admin - AI config", description = "UC-C04 Cấu hình AI & guardrail")
@RestController
@RequestMapping("/api/admin/ai-config")
@RequiredArgsConstructor
public class AdminAiConfigController {

    private final AiConfigService aiConfigService;

    @Operation(summary = "Xem cấu hình, model đang hiệu lực, trạng thái kết nối AI, lịch sử thay đổi")
    @GetMapping
    public ApiResponse<AiConfigResponse> get() {
        return ApiResponse.ok(aiConfigService.get());
    }

    @Operation(summary = "Cập nhật một hoặc nhiều tham số",
            description = "Khóa hợp lệ: llm.model, llm.temperature [0–1], rag.top_k [1–20], rag.top_n [1–10], "
                    + "rag.score_threshold [0–1], stt.language, chat.disclaimer, chat.blocked_topics (phân tách bằng dấu phẩy)")
    @PutMapping
    public ApiResponse<AiConfigResponse> update(@CurrentUser UserPrincipal admin, @RequestBody Map<String, String> values) {
        return ApiResponse.ok(aiConfigService.update(admin.id(), values), "Đã lưu cấu hình");
    }

    @Operation(summary = "AF-1 — Khôi phục mặc định")
    @PostMapping("/reset")
    public ApiResponse<AiConfigResponse> reset(@CurrentUser UserPrincipal admin) {
        return ApiResponse.ok(aiConfigService.resetDefaults(admin.id()), "Đã khôi phục cấu hình mặc định");
    }
}
