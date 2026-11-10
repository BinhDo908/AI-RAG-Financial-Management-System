package com.fintech.chatbot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.dao.AiDataPermissionDao;
import com.fintech.chatbot.dao.BudgetDao;
import com.fintech.chatbot.dao.CategoryDao;
import com.fintech.chatbot.dao.ConversationDao;
import com.fintech.chatbot.dao.DebtDao;
import com.fintech.chatbot.dao.FeedbackDao;
import com.fintech.chatbot.dao.FinancialProfileDao;
import com.fintech.chatbot.dao.MessageDao;
import com.fintech.chatbot.dao.NotificationDao;
import com.fintech.chatbot.dao.RecurringRuleDao;
import com.fintech.chatbot.dao.SavingGoalDao;
import com.fintech.chatbot.dao.TransactionDao;
import com.fintech.chatbot.dao.UserDao;
import com.fintech.chatbot.dao.WalletDao;
import com.fintech.chatbot.dto.request.ChangePasswordRequest;
import com.fintech.chatbot.dto.request.DeleteAccountRequest;
import com.fintech.chatbot.dto.request.UpdateProfileRequest;
import com.fintech.chatbot.dto.response.UserResponse;
import com.fintech.chatbot.entity.User;
import com.fintech.chatbot.entity.enums.Role;
import com.fintech.chatbot.mapper.UserMapper;
import com.fintech.chatbot.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserDao userDao;
    private final WalletDao walletDao;
    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;
    private final SavingGoalDao savingGoalDao;
    private final DebtDao debtDao;
    private final RecurringRuleDao recurringRuleDao;
    private final FinancialProfileDao financialProfileDao;
    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final FeedbackDao feedbackDao;
    private final NotificationDao notificationDao;
    private final AiDataPermissionDao aiDataPermissionDao;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userMapper.toResponse(loadUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = loadUser(userId);
        user.setFullName(request.fullName().trim());
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = loadUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.WRONG_CURRENT_PASSWORD);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.bumpTokenVersion();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportData(Long userId) {
        Map<String, Object> files = new LinkedHashMap<>();
        files.put("profile.json", userMapper.toResponse(loadUser(userId)));
        files.put("wallets.json", walletDao.findAllByUserId(userId));
        files.put("categories.json", categoryDao.findOwnedByUser(userId));
        files.put("transactions.json", transactionDao.findAllByUserId(userId));
        files.put("budgets.json", budgetDao.findAllByUserId(userId));
        files.put("saving_goals.json", savingGoalDao.findAllByUserId(userId));
        files.put("debts.json", debtDao.findAllByUserId(userId));
        files.put("recurring_rules.json", recurringRuleDao.findAllByUserId(userId));
        files.put("financial_profile.json", financialProfileDao.findByUserId(userId).orElse(null));
        files.put("ai_data_permission.json", aiDataPermissionDao.findByUserId(userId).orElse(null));
        files.put("conversations.json", conversationDao.findAllByUserId(userId));
        files.put("messages.json", messageDao.findAllByUserId(userId));
        files.put("feedbacks.json", feedbackDao.findAllByUserId(userId));
        files.put("notifications.json", notificationDao.findAllByUserId(userId));
        return zipAsJson(files);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = loadUser(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Tài khoản quản trị không thể tự xóa");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.WRONG_CURRENT_PASSWORD);
        }
        if (!DeleteAccountRequest.CONFIRMATION_TEXT.equals(request.confirmation().trim())) {
            throw new BusinessException(ErrorCode.DELETE_CONFIRMATION_INVALID,
                    "Vui lòng gõ chính xác: " + DeleteAccountRequest.CONFIRMATION_TEXT);
        }
        userDao.hardDelete(userId);
    }

    private User loadUser(Long userId) {
        return userDao.findById(userId).orElseThrow(() -> BusinessException.notFound("tài khoản"));
    }

    private byte[] zipAsJson(Map<String, Object> files) {
        var writer = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writer();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, Object> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(writer.writeValueAsBytes(file.getValue()));
                zip.closeEntry();
            }
            zip.finish();
            return bytes.toByteArray();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể chuyển dữ liệu sang JSON", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
