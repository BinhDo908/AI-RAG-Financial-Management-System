package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.util.MoneyFormatter;
import com.fintech.chatbot.dao.DebtDao;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dto.request.DebtCreateRequest;
import com.fintech.chatbot.dto.request.DebtUpdateRequest;
import com.fintech.chatbot.dto.request.WalletAmountRequest;
import com.fintech.chatbot.dto.response.DebtResponse;
import com.fintech.chatbot.dto.response.DebtSummaryResponse;
import com.fintech.chatbot.dto.response.TransactionResponse;
import com.fintech.chatbot.entity.Debt;
import com.fintech.chatbot.entity.Transaction;
import com.fintech.chatbot.entity.enums.DebtStatus;
import com.fintech.chatbot.entity.enums.DebtType;
import com.fintech.chatbot.entity.enums.NotificationType;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.mapper.FinanceMapper;
import com.fintech.chatbot.service.DebtService;
import com.fintech.chatbot.service.support.FinanceLookup;
import com.fintech.chatbot.service.support.NotificationPublisher;
import com.fintech.chatbot.service.support.TransactionRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtServiceImpl implements DebtService {

    private static final int DUE_SOON_DAYS = 7;
    private static final int REMIND_DAYS_BEFORE = 3;

    private final DebtDao debtDao;
    private final TransactionDao transactionDao;
    private final FinanceLookup lookup;
    private final TransactionRecorder recorder;
    private final NotificationPublisher notificationPublisher;
    private final FinanceMapper mapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<DebtResponse> list(Long userId, DebtType type, DebtStatus status) {
        return debtDao.findByUser(userId, type, status).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DebtResponse get(Long userId, Long debtId) {
        return toResponse(load(userId, debtId));
    }

    @Override
    @Transactional(readOnly = true)
    public DebtSummaryResponse summary(Long userId) {
        LocalDate limit = LocalDate.now(clock).plusDays(DUE_SOON_DAYS);
        List<DebtResponse> dueSoon = debtDao.findByUser(userId, null, DebtStatus.OPEN).stream()
                .filter(d -> d.getDueDate() != null && !d.getDueDate().isAfter(limit))
                .map(this::toResponse)
                .toList();
        return new DebtSummaryResponse(debtDao.sumRemaining(userId, DebtType.BORROW),
                debtDao.sumRemaining(userId, DebtType.LEND), dueSoon);
    }

    @Override
    @Transactional
    public DebtResponse create(Long userId, DebtCreateRequest request) {
        LocalDate start = request.startDate() == null ? LocalDate.now(clock) : request.startDate();
        if (request.dueDate() != null && request.dueDate().isBefore(start)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "Hạn trả phải sau ngày bắt đầu");
        }
        Debt debt = new Debt();
        debt.setUserId(userId);
        debt.setType(request.type());
        debt.setCounterparty(request.counterparty().trim());
        debt.setPrincipal(request.principal());
        debt.setRemaining(request.principal());
        debt.setStartDate(start);
        debt.setDueDate(request.dueDate());
        debt.setNote(request.note());
        debtDao.save(debt);

        boolean borrow = request.type() == DebtType.BORROW;
        recorder.record(userId, TransactionRecorder.Draft.builder()
                .type(borrow ? TransactionType.INCOME : TransactionType.EXPENSE)
                .amount(request.principal())
                .wallet(lookup.activeWallet(userId, request.walletId()))
                .date(start)
                .note((borrow ? "Vay của " : "Cho vay: ") + debt.getCounterparty())
                .source(TransactionSource.MANUAL)
                .debt(debt)
                .build());
        return toResponse(debt);
    }

    @Override
    @Transactional
    public DebtResponse update(Long userId, Long debtId, DebtUpdateRequest request) {
        Debt debt = load(userId, debtId);
        if (request.dueDate() != null && request.dueDate().isBefore(debt.getStartDate())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "Hạn trả phải sau ngày bắt đầu");
        }
        debt.setCounterparty(request.counterparty().trim());
        debt.setDueDate(request.dueDate());
        debt.setNote(request.note());
        return toResponse(debt);
    }

    @Override
    @Transactional
    public DebtResponse recordPayment(Long userId, Long debtId, WalletAmountRequest request) {
        Debt debt = load(userId, debtId);
        if (debt.getStatus() == DebtStatus.PAID) {
            throw new BusinessException(ErrorCode.DEBT_ALREADY_PAID);
        }
        if (request.amount().compareTo(debt.getRemaining()) > 0) {
            throw new BusinessException(ErrorCode.DEBT_PAYMENT_EXCEEDS);
        }
        boolean borrow = debt.getType() == DebtType.BORROW;
        recorder.record(userId, TransactionRecorder.Draft.builder()
                .type(borrow ? TransactionType.EXPENSE : TransactionType.INCOME)
                .amount(request.amount())
                .wallet(lookup.activeWallet(userId, request.walletId()))
                .date(request.date() == null ? LocalDate.now(clock) : request.date())
                .note(request.note() != null && !request.note().isBlank() ? request.note()
                        : (borrow ? "Trả nợ " : "Thu nợ từ ") + debt.getCounterparty())
                .source(TransactionSource.MANUAL)
                .debt(debt)
                .build());
        debt.setRemaining(debt.getRemaining().subtract(request.amount()));
        if (debt.getRemaining().signum() == 0) {
            debt.setStatus(DebtStatus.PAID);
        }
        return toResponse(debt);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long debtId) {
        Debt debt = load(userId, debtId);
        for (Transaction t : transactionDao.findByDebt(userId, debtId)) {
            recorder.remove(t);
        }
        debtDao.delete(debt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> transactions(Long userId, Long debtId) {
        load(userId, debtId);
        return mapper.toTransactionResponses(transactionDao.findByDebt(userId, debtId));
    }

    @Override
    @Transactional
    public int notifyDueDebts(LocalDate today) {
        int count = 0;
        for (Debt d : debtDao.findOpenDueOn(List.of(today, today.plusDays(REMIND_DAYS_BEFORE)))) {
            boolean dueToday = d.getDueDate().equals(today);
            String what = d.getType() == DebtType.BORROW
                    ? "Khoản vay của %s".formatted(d.getCounterparty())
                    : "Khoản cho %s vay".formatted(d.getCounterparty());
            String when = dueToday ? "đến hạn hôm nay" : "đến hạn sau %d ngày".formatted(REMIND_DAYS_BEFORE);
            notificationPublisher.publish(d.getUserId(), NotificationType.DEBT_DUE,
                    "%s %s, còn %s".formatted(what, when, MoneyFormatter.format(d.getRemaining())), d.getId());
            count++;
        }
        return count;
    }

    private DebtResponse toResponse(Debt d) {
        boolean overdue = d.getStatus() == DebtStatus.OPEN && d.getDueDate() != null
                && d.getDueDate().isBefore(LocalDate.now(clock));
        return new DebtResponse(d.getId(), d.getType(), d.getCounterparty(), d.getPrincipal(), d.getRemaining(),
                d.getPrincipal().subtract(d.getRemaining()), d.getStartDate(), d.getDueDate(), d.getNote(),
                d.getStatus(), overdue);
    }

    private Debt load(Long userId, Long debtId) {
        return debtDao.findByIdAndUserId(debtId, userId).orElseThrow(() -> BusinessException.notFound("khoản nợ"));
    }
}
