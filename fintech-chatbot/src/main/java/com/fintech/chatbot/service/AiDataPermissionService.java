package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.AiDataPermissionRequest;
import com.fintech.chatbot.dto.response.AiDataPermissionResponse;

/**
 * UC-D05 — Quyền dữ liệu cho AI (BR-05). Giai đoạn 3 đọc quyền này trước khi AI truy cập dữ liệu.
 */
public interface AiDataPermissionService {

    AiDataPermissionResponse get(Long userId);

    AiDataPermissionResponse update(Long userId, AiDataPermissionRequest request);
}
