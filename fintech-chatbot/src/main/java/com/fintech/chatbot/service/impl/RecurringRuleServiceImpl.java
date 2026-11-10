package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.util.MoneyFormatter;
import com.fintech.chatbot.dao.RecurringRuleDao;
import com.fintech.chatbot.dto.request.RecurringRuleRequest;
import com.fintech.chatbot.dto.response.RecurringRuleResponse;
import com.fintech.chatbot.dto.response.TransactionResult;
import com.fintech.chatbot.entity.RecurringRule;
import com.fintech.chatbot.entity.Transaction;
import com.fintech.chatbot.entity.enums.NotificationType;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.mapper.FinanceMapper;
import com.fintech.chatbot.service.RecurringRuleService;
import com.fintech.chatbot.service.support.FinanceLookup;
import com.fintech.chatbot.service.support.NotificationPublisher;
import com.fintech.chatbot.service.support.Recurrence;
import com.fintech.chatbot.service.support.TransactionRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringRuleServiceImpl implements RecurringRuleService {

    /** Số kỳ ghi bù tối đa trong một lần chạy (phòng trường hợp server tắt lâu). */
    private static final int MAX_CATCH_UP = 31;

    private final RecurringRuleDao ruleDao;
    private final FinanceLookup lookup;
    private final TransactionRecorder recorder;
    private final NotificationPublisher notificationPublisher;
    private final FinanceMapper mapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<RecurringRuleResponse> list(Long userId) {
        return ruleDao.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(RecurringRule::isActive).reversed()
                        .thenComparing(RecurringRule::getNextRunDate))
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringRuleResponse get(Long userId, Long ruleId) {
        return mapper.toResponse(load(userId, ruleId));
    }

    @Override
    @Transactional
    public RecurringRuleResponse create(Long userId, RecurringRuleRequest request) {
        RecurringRule rule = new RecurringRule();
        rule.setUserId(userId);
        apply(userId, rule, request);
        ruleDao.save(rule);
        return mapper.toResponse(rule);
    }

    @Override
    @Transactional
    public RecurringRuleResponse update(Long userId, Long ruleId, RecurringRuleRequest request) {
        RecurringRule rule = load(userId, ruleId);
        apply(userId, rule, request);
        return mapper.toResponse(rule);
    }

    @Override
    @Transactional
    public RecurringRuleResponse setActive(Long userId, Long ruleId, boolean active) {
        RecurringRule rule = load(userId, ruleId);
        if (active && !rule.isActive()) {
            rule.setNextRunDate(Recurrence.firstOnOrAfter(rule.getStartDate(), rule.getFrequency(), today()));
        }
        rule.setActive(active);
        return mapper.toResponse(rule);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long ruleId) {
        ruleDao.delete(load(userId, ruleId));
    }

    @Override
    @Transactional
    public TransactionResult execute(Long userId, Long ruleId, LocalDate date) {
        RecurringRule rule = load(userId, ruleId);
        Transaction t = createTransaction(rule, date == null ? today() : date);
        return new TransactionResult(mapper.toResponse(t), t.getWallet().getCurrentBalance(),
                t.getWallet().getCurrentBalance().signum() < 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findDueRuleIds(LocalDate today) {
        return ruleDao.findDueIds(today);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDue(Long ruleId, LocalDate today) {
        RecurringRule rule = ruleDao.findById(ruleId).orElse(null);
        if (rule == null || !rule.isActive() || rule.getNextRunDate().isAfter(today)) {
            return;
        }

        // EX-1: ví đã lưu trữ → không tạo giao dịch, yêu cầu user cập nhật quy tắc
        if (rule.getWallet().isArchived()) {
            notificationPublisher.publish(rule.getUserId(), NotificationType.RECURRING,
                    "Không thể ghi \"%s\" vì ví %s đã được lưu trữ. Hãy cập nhật quy tắc định kỳ."
                            .formatted(describe(rule), rule.getWallet().getName()), rule.getId());
            rule.setNextRunDate(Recurrence.firstAfter(rule.getStartDate(), rule.getFrequency(), today));
            return;
        }

        if (!rule.isAutoCreate()) {
            notificationPublisher.publish(rule.getUserId(), NotificationType.RECURRING,
                    "Đến hạn %s: %s. Bấm để xác nhận ghi.".formatted(describe(rule),
                            MoneyFormatter.format(rule.getAmount())), rule.getId());
            rule.setNextRunDate(Recurrence.firstAfter(rule.getStartDate(), rule.getFrequency(), today));
            return;
        }

        int created = 0;
        while (!rule.getNextRunDate().isAfter(today) && created < MAX_CATCH_UP) {
            createTransaction(rule, rule.getNextRunDate());
            created++;
            rule.setNextRunDate(Recurrence.firstAfter(rule.getStartDate(), rule.getFrequency(), rule.getNextRunDate()));
        }
        if (rule.getNextRunDate().isBefore(today) || rule.getNextRunDate().equals(today)) {
            rule.setNextRunDate(Recurrence.firstAfter(rule.getStartDate(), rule.getFrequency(), today));
        }
        notificationPublisher.publish(rule.getUserId(), NotificationType.RECURRING,
                "Đã tự ghi %s: %s%s".formatted(describe(rule), MoneyFormatter.format(rule.getAmount()),
                        created > 1 ? " (" + created + " kỳ)" : ""), rule.getId());
    }

    private Transaction createTransaction(RecurringRule rule, LocalDate date) {
        return recorder.record(rule.getUserId(), TransactionRecorder.Draft.builder()
                .type(rule.getType())
                .amount(rule.getAmount())
                .wallet(lookup.activeWallet(rule.getUserId(), rule.getWalletId()))
                .category(rule.getCategory())
                .date(date)
                .note(rule.getNote())
                .source(TransactionSource.RECURRING)
                .build());
    }

    private void apply(Long userId, RecurringRule rule, RecurringRuleRequest r) {
        if (r.type() != TransactionType.INCOME && r.type() != TransactionType.EXPENSE) {
            throw new BusinessException(ErrorCode.TRANSACTION_TYPE_NOT_ALLOWED,
                    "Giao dịch định kỳ chỉ hỗ trợ thu hoặc chi");
        }
        rule.assignWallet(lookup.activeWallet(userId, r.walletId()));
        rule.assignCategory(lookup.categoryFor(userId, r.categoryId(), r.type()));
        rule.setType(r.type());
        rule.setAmount(r.amount());
        rule.setNote(r.note());
        rule.setFrequency(r.frequency());
        rule.setStartDate(r.startDate());
        rule.setAutoCreate(r.autoCreate());
        rule.setNextRunDate(Recurrence.firstOnOrAfter(r.startDate(), r.frequency(), today()));
    }

    private static String describe(RecurringRule rule) {
        if (rule.getNote() != null && !rule.getNote().isBlank()) {
            return rule.getNote();
        }
        return rule.getCategory() != null ? rule.getCategory().getName() : "giao dịch định kỳ";
    }

    private RecurringRule load(Long userId, Long ruleId) {
        return ruleDao.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> BusinessException.notFound("quy tắc định kỳ"));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
