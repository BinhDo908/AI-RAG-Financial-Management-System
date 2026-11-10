package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.DebtCreateRequest;
import com.fintech.chatbot.dto.request.DebtUpdateRequest;
import com.fintech.chatbot.dto.request.WalletAmountRequest;
import com.fintech.chatbot.dto.response.DebtResponse;
import com.fintech.chatbot.dto.response.DebtSummaryResponse;
import com.fintech.chatbot.dto.response.TransactionResponse;
import com.fintech.chatbot.entity.enums.DebtStatus;
import com.fintech.chatbot.entity.enums.DebtType;

import java.time.LocalDate;
import java.util.List;

/**
 * UC-A06 — Sổ nợ: đi vay / cho vay.
 */
public interface DebtService {

    List<DebtResponse> list(Long userId, DebtType type, DebtStatus status);

    DebtResponse get(Long userId, Long debtId);

    /** AF-1: tổng đang nợ, tổng đang cho vay, khoản đến hạn trong 7 ngày tới (kể cả quá hạn). */
    DebtSummaryResponse summary(Long userId);

    /** Bước 2: Đi vay = tiền vào ví (thu); Cho vay = tiền ra khỏi ví (chi). */
    DebtResponse create(Long userId, DebtCreateRequest request);

    DebtResponse update(Long userId, Long debtId, DebtUpdateRequest request);

    /** Bước 3: trả nợ (với khoản đi vay) hoặc thu nợ (với khoản cho vay). */
    DebtResponse recordPayment(Long userId, Long debtId, WalletAmountRequest request);

    /** Xóa khoản nợ cùng mọi giao dịch gắn với nó, tính lại số dư ví. */
    void delete(Long userId, Long debtId);

    List<TransactionResponse> transactions(Long userId, Long debtId);

    /** UC-D04: nhắc các khoản đến hạn sau 3 ngày và đến hạn hôm nay (mọi user). @return số thông báo đã tạo. */
    int notifyDueDebts(LocalDate today);
}
