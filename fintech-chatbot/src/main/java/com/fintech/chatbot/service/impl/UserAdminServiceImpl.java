package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.common.response.PageResponse;
import com.fintech.chatbot.dao.UserDao;
import com.fintech.chatbot.dto.response.ResetPasswordResponse;
import com.fintech.chatbot.dto.response.UserResponse;
import com.fintech.chatbot.entity.User;
import com.fintech.chatbot.entity.enums.Role;
import com.fintech.chatbot.entity.enums.UserStatus;
import com.fintech.chatbot.mapper.UserMapper;
import com.fintech.chatbot.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final int MAX_PAGE_SIZE = 100;

    private final SecureRandom random = new SecureRandom();

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String keyword, UserStatus status, Role role, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        var criteria = new UserDao.SearchCriteria(keyword, status, role);
        var items = userDao.search(criteria, safePage, safeSize).stream().map(userMapper::toResponse).toList();
        return PageResponse.of(items, safePage, safeSize, userDao.countSearch(criteria));
    }

    @Override
    @Transactional
    public UserResponse lock(Long adminId, Long userId) {
        if (adminId.equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_LOCK_SELF);
        }
        User user = loadUser(userId);
        user.setStatus(UserStatus.LOCKED);
        user.bumpTokenVersion();
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse unlock(Long userId) {
        User user = loadUser(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setLoginLockedUntil(null);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(Long userId) {
        User user = loadUser(userId);
        String temporary = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setFailedLoginCount(0);
        user.setLoginLockedUntil(null);
        user.bumpTokenVersion();
        return new ResetPasswordResponse(user.getId(), temporary);
    }

    @Override
    @Transactional
    public boolean ensureAdmin(String email, String rawPassword, String fullName) {
        if (userDao.existsByEmail(email)) {
            return false;
        }
        User admin = new User();
        admin.setEmail(email.trim().toLowerCase(Locale.ROOT));
        admin.setFullName(fullName);
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        userDao.save(admin);
        return true;
    }

    private User loadUser(Long userId) {
        return userDao.findById(userId).orElseThrow(() -> BusinessException.notFound("tài khoản"));
    }

    /** Luôn có cả chữ và số để thỏa quy tắc mật khẩu. */
    private String generateTemporaryPassword() {
        String all = LETTERS + DIGITS;
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        while (sb.length() < TEMP_PASSWORD_LENGTH) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }
        return sb.toString();
    }
}
