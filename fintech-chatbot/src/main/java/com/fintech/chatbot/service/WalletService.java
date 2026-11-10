package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.AdjustBalanceRequest;
import com.fintech.chatbot.dto.request.WalletCreateRequest;
import com.fintech.chatbot.dto.request.WalletUpdateRequest;
import com.fintech.chatbot.dto.response.TransactionResult;
import com.fintech.chatbot.dto.response.WalletListResponse;
import com.fintech.chatbot.dto.response.WalletResponse;

/**
 * UC-A01 — Quản lý ví.
 */
public interface WalletService {

    /** Tổng số dư chỉ tính ví chưa lưu trữ. */
    WalletListResponse list(Long userId, boolean includeArchived);

    WalletResponse get(Long userId, Long walletId);

    WalletResponse create(Long userId, WalletCreateRequest request);

    WalletResponse update(Long userId, Long walletId, WalletUpdateRequest request);

    /** AF-2: ví lưu trữ không chọn được khi nhập giao dịch, giao dịch cũ vẫn giữ. */
    WalletResponse setArchived(Long userId, Long walletId, boolean archived);

    /** AF-3: chỉ xóa được ví chưa có giao dịch / quy tắc định kỳ. */
    void delete(Long userId, Long walletId);

    /** AF-1 / BR-07: tạo giao dịch ADJUST bằng chênh lệch giữa số dư mới và số dư hiện tại. */
    TransactionResult adjustBalance(Long userId, Long walletId, AdjustBalanceRequest request);
}
