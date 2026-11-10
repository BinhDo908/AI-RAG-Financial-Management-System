package com.fintech.chatbot.service.support;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.CategoryDao;
import com.fintech.chatbot.dao.WalletDao;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.Wallet;
import com.fintech.chatbot.entity.enums.CategoryType;
import com.fintech.chatbot.entity.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Nạp ví / danh mục theo quyền sở hữu và kiểm tra điều kiện dùng chung cho các service tài chính.
 * Không tìm thấy hoặc không thuộc user đều trả lỗi NOT_FOUND (không tiết lộ dữ liệu người khác).
 */
@Component
@RequiredArgsConstructor
public class FinanceLookup {

    private final WalletDao walletDao;
    private final CategoryDao categoryDao;

    public Wallet wallet(Long userId, Long walletId) {
        return walletDao.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> BusinessException.notFound("ví"));
    }

    /** Ví dùng cho giao dịch mới: phải chưa lưu trữ. */
    public Wallet activeWallet(Long userId, Long walletId) {
        Wallet wallet = wallet(userId, walletId);
        if (wallet.isArchived()) {
            throw new BusinessException(ErrorCode.WALLET_ARCHIVED);
        }
        return wallet;
    }

    /** Danh mục mặc định hoặc của chính user. */
    public Category category(Long userId, Long categoryId) {
        return categoryDao.findAccessible(categoryId, userId)
                .orElseThrow(() -> BusinessException.notFound("danh mục"));
    }

    /** Danh mục phù hợp với loại giao dịch thu / chi. */
    public Category categoryFor(Long userId, Long categoryId, TransactionType type) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.CATEGORY_REQUIRED);
        }
        Category category = category(userId, categoryId);
        CategoryType expected = type == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;
        if (category.getType() != expected) {
            throw new BusinessException(ErrorCode.CATEGORY_TYPE_MISMATCH);
        }
        return category;
    }
}
