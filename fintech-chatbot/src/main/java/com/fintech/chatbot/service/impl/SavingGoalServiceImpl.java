package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.util.MoneyFormatter;
import com.fintech.chatbot.dao.SavingGoalDao;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dto.request.SavingGoalRequest;
import com.fintech.chatbot.dto.request.WalletAmountRequest;
import com.fintech.chatbot.dto.response.SavingGoalResponse;
import com.fintech.chatbot.dto.response.TransactionResponse;
import com.fintech.chatbot.entity.SavingGoal;
import com.fintech.chatbot.entity.enums.GoalStatus;
import com.fintech.chatbot.entity.enums.NotificationType;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.mapper.FinanceMapper;
import com.fintech.chatbot.service.SavingGoalService;
import com.fintech.chatbot.service.support.FinanceLookup;
import com.fintech.chatbot.service.support.NotificationPublisher;
import com.fintech.chatbot.service.support.TransactionRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingGoalServiceImpl implements SavingGoalService {

    private final SavingGoalDao goalDao;
    private final TransactionDao transactionDao;
    private final FinanceLookup lookup;
    private final TransactionRecorder recorder;
    private final NotificationPublisher notificationPublisher;
    private final FinanceMapper mapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<SavingGoalResponse> list(Long userId) {
        return goalDao.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(SavingGoal::getStatus).thenComparing(SavingGoal::getId))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SavingGoalResponse get(Long userId, Long goalId) {
        return toResponse(load(userId, goalId));
    }

    @Override
    @Transactional
    public SavingGoalResponse create(Long userId, SavingGoalRequest request) {
        SavingGoal goal = new SavingGoal();
        goal.setUserId(userId);
        apply(goal, request);
        goalDao.save(goal);
        return toResponse(goal);
    }

    @Override
    @Transactional
    public SavingGoalResponse update(Long userId, Long goalId, SavingGoalRequest request) {
        SavingGoal goal = loadActive(userId, goalId);
        apply(goal, request);
        refreshStatus(goal);
        return toResponse(goal);
    }

    @Override
    @Transactional
    public SavingGoalResponse cancel(Long userId, Long goalId) {
        SavingGoal goal = load(userId, goalId);
        goal.setStatus(GoalStatus.CANCELLED);
        return toResponse(goal);
    }

    @Override
    @Transactional
    public SavingGoalResponse deposit(Long userId, Long goalId, WalletAmountRequest request) {
        SavingGoal goal = loadActive(userId, goalId);
        recorder.record(userId, TransactionRecorder.Draft.builder()
                .type(TransactionType.EXPENSE)
                .amount(request.amount())
                .wallet(lookup.activeWallet(userId, request.walletId()))
                .date(dateOrToday(request.date()))
                .note(noteOr(request.note(), "Nạp vào mục tiêu: " + goal.getName()))
                .source(TransactionSource.MANUAL)
                .goal(goal)
                .build());
        goal.setCurrentAmount(goal.getCurrentAmount().add(request.amount()));
        refreshStatus(goal);
        return toResponse(goal);
    }

    @Override
    @Transactional
    public SavingGoalResponse withdraw(Long userId, Long goalId, WalletAmountRequest request) {
        SavingGoal goal = loadActive(userId, goalId);
        if (request.amount().compareTo(goal.getCurrentAmount()) > 0) {
            throw new BusinessException(ErrorCode.GOAL_WITHDRAW_EXCEEDS);
        }
        recorder.record(userId, TransactionRecorder.Draft.builder()
                .type(TransactionType.INCOME)
                .amount(request.amount())
                .wallet(lookup.activeWallet(userId, request.walletId()))
                .date(dateOrToday(request.date()))
                .note(noteOr(request.note(), "Rút từ mục tiêu: " + goal.getName()))
                .source(TransactionSource.MANUAL)
                .goal(goal)
                .build());
        goal.setCurrentAmount(goal.getCurrentAmount().subtract(request.amount()));
        refreshStatus(goal);
        return toResponse(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> transactions(Long userId, Long goalId) {
        load(userId, goalId);
        return mapper.toTransactionResponses(transactionDao.findByGoal(userId, goalId));
    }

    /** POST-2: đạt mục tiêu → COMPLETED + thông báo chúc mừng; rút xuống dưới mục tiêu → IN_PROGRESS. */
    private void refreshStatus(SavingGoal goal) {
        boolean reached = goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0;
        if (reached && goal.getStatus() == GoalStatus.IN_PROGRESS) {
            goal.setStatus(GoalStatus.COMPLETED);
            notificationPublisher.publish(goal.getUserId(), NotificationType.GOAL_COMPLETED,
                    "Chúc mừng! Bạn đã đạt mục tiêu \"%s\" (%s)".formatted(
                            goal.getName(), MoneyFormatter.format(goal.getTargetAmount())),
                    goal.getId());
        } else if (!reached && goal.getStatus() == GoalStatus.COMPLETED) {
            goal.setStatus(GoalStatus.IN_PROGRESS);
        }
    }

    private SavingGoalResponse toResponse(SavingGoal g) {
        LocalDate today = LocalDate.now(clock);
        BigDecimal remaining = g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO);
        int percent = g.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                .divide(g.getTargetAmount(), 0, RoundingMode.DOWN).min(BigDecimal.valueOf(100)).intValue();
        boolean overdue = g.getDeadline() != null && g.getDeadline().isBefore(today)
                && g.getStatus() == GoalStatus.IN_PROGRESS;
        return new SavingGoalResponse(g.getId(), g.getName(), g.getTargetAmount(), g.getCurrentAmount(),
                remaining, percent, g.getDeadline(), g.getStatus(), monthlyNeeded(g, remaining, today), overdue);
    }

    /** Số tiền cần để dành mỗi tháng = còn thiếu / số tháng còn lại (làm tròn lên, tối thiểu 1 tháng). */
    private static BigDecimal monthlyNeeded(SavingGoal g, BigDecimal remaining, LocalDate today) {
        if (g.getDeadline() == null || remaining.signum() == 0 || g.getStatus() != GoalStatus.IN_PROGRESS
                || g.getDeadline().isBefore(today)) {
            return null;
        }
        long months = ChronoUnit.MONTHS.between(today, g.getDeadline());
        if (today.plusMonths(months).isBefore(g.getDeadline())) {
            months++;
        }
        months = Math.max(months, 1);
        return remaining.divide(BigDecimal.valueOf(months), 0, RoundingMode.UP);
    }

    private void apply(SavingGoal goal, SavingGoalRequest request) {
        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setDeadline(request.deadline());
    }

    private SavingGoal load(Long userId, Long goalId) {
        return goalDao.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> BusinessException.notFound("mục tiêu tiết kiệm"));
    }

    private SavingGoal loadActive(Long userId, Long goalId) {
        SavingGoal goal = load(userId, goalId);
        if (goal.getStatus() == GoalStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.GOAL_NOT_ACTIVE);
        }
        return goal;
    }

    private LocalDate dateOrToday(LocalDate date) {
        return date == null ? LocalDate.now(clock) : date;
    }

    private static String noteOr(String note, String fallback) {
        return note == null || note.isBlank() ? fallback : note;
    }
}
