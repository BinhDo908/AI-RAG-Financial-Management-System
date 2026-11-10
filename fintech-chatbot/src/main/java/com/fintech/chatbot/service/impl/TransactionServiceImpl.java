package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dao.TransactionFilter;
import com.fintech.chatbot.dto.request.TransactionRequest;
import com.fintech.chatbot.dto.response.TransactionResponse;
import com.fintech.chatbot.dto.response.TransactionResult;
import com.fintech.chatbot.dto.response.TransactionSearchResponse;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.Transaction;
import com.fintech.chatbot.entity.Wallet;
import com.fintech.chatbot.entity.enums.TransactionSource;
import com.fintech.chatbot.entity.enums.TransactionType;
import com.fintech.chatbot.mapper.FinanceMapper;
import com.fintech.chatbot.service.TransactionService;
import com.fintech.chatbot.service.support.FinanceLookup;
import com.fintech.chatbot.service.support.TransactionRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionDao transactionDao;
    private final FinanceLookup lookup;
    private final TransactionRecorder recorder;
    private final FinanceMapper mapper;
    private final Clock clock;

    @Override
    @Transactional
    public TransactionResult create(Long userId, TransactionRequest request) {
        return create(userId, request, TransactionSource.MANUAL);
    }

    @Override
    @Transactional
    public TransactionResult create(Long userId, TransactionRequest request, TransactionSource source) {
        Transaction t = recorder.record(userId, toDraft(userId, request, source));
        return result(t);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse get(Long userId, Long transactionId) {
        return mapper.toResponse(load(userId, transactionId));
    }

    @Override
    @Transactional
    public TransactionResult update(Long userId, Long transactionId, TransactionRequest request) {
        Transaction t = loadEditable(userId, transactionId);
        recorder.update(t, toDraft(userId, request, t.getSource()));
        return result(t);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction t = load(userId, transactionId);
        if (t.isLinked()) {
            throw new BusinessException(ErrorCode.TRANSACTION_LINKED);
        }
        recorder.remove(t);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionSearchResponse search(Long userId, TransactionFilter filter, int page, int size) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        var items = mapper.toTransactionResponses(transactionDao.search(userId, filter, safePage, safeSize));
        var pageResponse = PageResponse.of(items, safePage, safeSize, transactionDao.countSearch(userId, filter));
        var totals = transactionDao.totals(userId, filter);
        return new TransactionSearchResponse(pageResponse, totals.income(), totals.expense(),
                totals.income().subtract(totals.expense()));
    }

    /** Kiểm tra dữ liệu theo UC-A02 EX-1 → EX-3 và chuyển thành bản nháp giao dịch. */
    private TransactionRecorder.Draft toDraft(Long userId, TransactionRequest r, TransactionSource source) {
        if (r.type() == TransactionType.ADJUST) {
            throw new BusinessException(ErrorCode.TRANSACTION_TYPE_NOT_ALLOWED,
                    "Điều chỉnh số dư thực hiện ở chức năng Ví");
        }
        Wallet wallet = lookup.activeWallet(userId, r.walletId());
        Wallet toWallet = null;
        Category category = null;
        if (r.type() == TransactionType.TRANSFER) {
            if (r.toWalletId() == null) {
                throw new BusinessException(ErrorCode.TRANSFER_TARGET_REQUIRED);
            }
            if (r.toWalletId().equals(r.walletId())) {
                throw new BusinessException(ErrorCode.TRANSFER_SAME_WALLET);
            }
            toWallet = lookup.activeWallet(userId, r.toWalletId());
        } else {
            category = lookup.categoryFor(userId, r.categoryId(), r.type());
        }
        return TransactionRecorder.Draft.builder()
                .type(r.type())
                .amount(r.amount())
                .wallet(wallet)
                .toWallet(toWallet)
                .category(category)
                .date(r.txnDate() == null ? LocalDate.now(clock) : r.txnDate())
                .note(r.note())
                .source(source)
                .build();
    }

    private Transaction load(Long userId, Long transactionId) {
        return transactionDao.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> BusinessException.notFound("giao dịch"));
    }

    private Transaction loadEditable(Long userId, Long transactionId) {
        Transaction t = load(userId, transactionId);
        if (t.isLinked()) {
            throw new BusinessException(ErrorCode.TRANSACTION_LINKED);
        }
        if (t.getType() == TransactionType.ADJUST) {
            throw new BusinessException(ErrorCode.TRANSACTION_TYPE_NOT_ALLOWED,
                    "Giao dịch điều chỉnh số dư chỉ có thể xóa, không sửa");
        }
        return t;
    }

    private TransactionResult result(Transaction t) {
        Wallet wallet = t.getWallet();
        return new TransactionResult(mapper.toResponse(t), wallet.getCurrentBalance(),
                wallet.getCurrentBalance().signum() < 0);
    }
}
