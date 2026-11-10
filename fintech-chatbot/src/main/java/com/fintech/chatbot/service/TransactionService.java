package com.fintech.chatbot.service;

import com.fintech.chatbot.dao.TransactionFilter;
import com.fintech.chatbot.dto.request.TransactionRequest;
import com.fintech.chatbot.dto.response.TransactionResponse;
import com.fintech.chatbot.dto.response.TransactionResult;
import com.fintech.chatbot.dto.response.TransactionSearchResponse;
import com.fintech.chatbot.entity.enums.TransactionSource;

/**
 * UC-A02 Quản lý giao dịch, UC-A08 Tìm kiếm & lọc.
 */
public interface TransactionService {

    TransactionResult create(Long userId, TransactionRequest request);

    /** Dùng chung cho nhập liệu bằng chat / giọng nói ở giai đoạn 3 (UC-A11, A12). */
    TransactionResult create(Long userId, TransactionRequest request, TransactionSource source);

    TransactionResponse get(Long userId, Long transactionId);

    TransactionResult update(Long userId, Long transactionId, TransactionRequest request);

    void delete(Long userId, Long transactionId);

    TransactionSearchResponse search(Long userId, TransactionFilter filter, int page, int size);
}
