package com.fintech.chatbot.service;

import com.fintech.chatbot.dto.request.RecurringRuleRequest;
import com.fintech.chatbot.dto.response.RecurringRuleResponse;
import com.fintech.chatbot.dto.response.TransactionResult;

import java.time.LocalDate;
import java.util.List;

/**
 * UC-A07 — Giao dịch định kỳ.
 */
public interface RecurringRuleService {

    List<RecurringRuleResponse> list(Long userId);

    RecurringRuleResponse get(Long userId, Long ruleId);

    /** Ngày chạy đầu tiên = lần lặp đầu tiên từ hôm nay trở đi (không ghi bù quá khứ). */
    RecurringRuleResponse create(Long userId, RecurringRuleRequest request);

    /** Sửa quy tắc không ảnh hưởng giao dịch đã sinh (AF-2). */
    RecurringRuleResponse update(Long userId, Long ruleId, RecurringRuleRequest request);

    /** AF-1: tạm dừng / bật lại; bật lại thì tính lại ngày chạy kế tiếp từ hôm nay. */
    RecurringRuleResponse setActive(Long userId, Long ruleId, boolean active);

    void delete(Long userId, Long ruleId);

    /** User bấm "Xác nhận ghi" từ thông báo nhắc: tạo giao dịch nguồn RECURRING. */
    TransactionResult execute(Long userId, Long ruleId, LocalDate date);

    /** UC-D04: id các quy tắc đang bật đã đến hạn (mọi user). */
    List<Long> findDueRuleIds(LocalDate today);

    /**
     * UC-D04: xử lý một quy tắc đến hạn trong transaction riêng.
     * Tự tạo giao dịch (ghi bù các kỳ bị lỡ) hoặc gửi một thông báo nhắc, rồi dời ngày chạy kế tiếp.
     */
    void processDue(Long ruleId, LocalDate today);
}
