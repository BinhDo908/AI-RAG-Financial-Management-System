package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.SavingGoalRequest;
import com.fintech.chatbot.dto.request.WalletAmountRequest;
import com.fintech.chatbot.dto.response.SavingGoalResponse;
import com.fintech.chatbot.dto.response.TransactionResponse;

import java.util.List;

/**
 * UC-A05 — Mục tiêu tiết kiệm.
 */
public interface SavingGoalService {

    List<SavingGoalResponse> list(Long userId);

    SavingGoalResponse get(Long userId, Long goalId);

    SavingGoalResponse create(Long userId, SavingGoalRequest request);

    SavingGoalResponse update(Long userId, Long goalId, SavingGoalRequest request);

    SavingGoalResponse cancel(Long userId, Long goalId);

    /** Bước 3: tạo giao dịch chi từ ví, gắn mục tiêu, cộng vào số tiền hiện có. */
    SavingGoalResponse deposit(Long userId, Long goalId, WalletAmountRequest request);

    /** AF-1: tạo giao dịch thu về ví, trừ khỏi mục tiêu. */
    SavingGoalResponse withdraw(Long userId, Long goalId, WalletAmountRequest request);

    List<TransactionResponse> transactions(Long userId, Long goalId);
}
