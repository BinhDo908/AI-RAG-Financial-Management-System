package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.RecurringRuleDao;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dao.WalletDao;
import com.fintech.chatbot.dto.request.AdjustBalanceRequest;
import com.fintech.chatbot.dto.request.WalletCreateRequest;
import com.fintech.chatbot.dto.request.WalletUpdateRequest;
import com.fintech.chatbot.dto.response.TransactionResult;
import com.fintech.chatbot.dto.response.WalletListResponse;
import com.fintech.chatbot.dto.response.WalletResponse;
import com.fintech.chatbot.entity.Transaction;
import com.fintech.chatbot.entity.Wallet;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.mapper.FinanceMapper;
import com.fintech.chatbot.service.WalletService;
import com.fintech.chatbot.service.support.FinanceLookup;
import com.fintech.chatbot.service.support.TransactionRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletDao walletDao;
    private final TransactionDao transactionDao;
    private final RecurringRuleDao recurringRuleDao;
    private final FinanceLookup lookup;
    private final TransactionRecorder recorder;
    private final FinanceMapper mapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public WalletListResponse list(Long userId, boolean includeArchived) {
        List<Wallet> wallets = walletDao.findAllByUserId(userId).stream()
                .filter(w -> includeArchived || !w.isArchived())
                .sorted(Comparator.comparing(Wallet::isArchived)
                        .thenComparing(Wallet::isDefaultWallet, Comparator.reverseOrder())
                        .thenComparing(Wallet::getId))
                .toList();
        BigDecimal total = wallets.stream()
                .filter(w -> !w.isArchived())
                .map(Wallet::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WalletListResponse(mapper.toWalletResponses(wallets), total);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse get(Long userId, Long walletId) {
        return mapper.toResponse(lookup.wallet(userId, walletId));
    }

    @Override
    @Transactional
    public WalletResponse create(Long userId, WalletCreateRequest request) {
        String name = request.name().trim();
        if (walletDao.existsByUserIdAndName(userId, name, null)) {
            throw new BusinessException(ErrorCode.WALLET_NAME_DUPLICATE);
        }
        boolean hasDefault = walletDao.findDefaultByUserId(userId).isPresent();
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setName(name);
        wallet.setType(request.type());
        wallet.setInitialBalance(request.initialBalance());
        wallet.setCurrentBalance(request.initialBalance());
        walletDao.save(wallet);
        if (request.defaultWallet() || !hasDefault) {
            makeDefault(userId, wallet);
        }
        return mapper.toResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse update(Long userId, Long walletId, WalletUpdateRequest request) {
        Wallet wallet = lookup.wallet(userId, walletId);
        String name = request.name().trim();
        if (walletDao.existsByUserIdAndName(userId, name, walletId)) {
            throw new BusinessException(ErrorCode.WALLET_NAME_DUPLICATE);
        }
        wallet.setName(name);
        wallet.setType(request.type());
        if (request.defaultWallet() && !wallet.isArchived()) {
            makeDefault(userId, wallet);
        }
        return mapper.toResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse setArchived(Long userId, Long walletId, boolean archived) {
        Wallet wallet = lookup.wallet(userId, walletId);
        wallet.setArchived(archived);
        if (archived && wallet.isDefaultWallet()) {
            wallet.setDefaultWallet(false);
            // Chuyển mặc định sang ví đang dùng khác (nếu có)
            walletDao.findAllByUserId(userId).stream()
                    .filter(w -> !w.isArchived() && !w.getId().equals(walletId))
                    .findFirst()
                    .ifPresent(w -> w.setDefaultWallet(true));
        }
        return mapper.toResponse(wallet);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long walletId) {
        Wallet wallet = lookup.wallet(userId, walletId);
        if (transactionDao.existsByWallet(walletId) || recurringRuleDao.existsByWallet(walletId)) {
            throw new BusinessException(ErrorCode.WALLET_HAS_TRANSACTIONS);
        }
        boolean wasDefault = wallet.isDefaultWallet();
        walletDao.delete(wallet);
        if (wasDefault) {
            walletDao.findAllByUserId(userId).stream()
                    .filter(w -> !w.isArchived())
                    .findFirst()
                    .ifPresent(w -> w.setDefaultWallet(true));
        }
    }

    @Override
    @Transactional
    public TransactionResult adjustBalance(Long userId, Long walletId, AdjustBalanceRequest request) {
        Wallet wallet = lookup.activeWallet(userId, walletId);
        BigDecimal difference = request.newBalance().subtract(wallet.getCurrentBalance());
        if (difference.signum() == 0) {
            throw new BusinessException(ErrorCode.BALANCE_UNCHANGED);
        }
        Transaction t = recorder.record(userId, TransactionRecorder.Draft.builder()
                .type(TransactionType.ADJUST)
                .amount(difference)
                .wallet(wallet)
                .date(request.date() == null ? LocalDate.now(clock) : request.date())
                .note(request.note() == null ? "Điều chỉnh số dư" : request.note())
                .source(TransactionSource.MANUAL)
                .build());
        return new TransactionResult(mapper.toResponse(t), wallet.getCurrentBalance(),
                wallet.getCurrentBalance().signum() < 0);
    }

    private void makeDefault(Long userId, Wallet wallet) {
        for (Wallet w : walletDao.findAllByUserId(userId)) {
            w.setDefaultWallet(w.getId().equals(wallet.getId()));
        }
        wallet.setDefaultWallet(true);
    }
}
