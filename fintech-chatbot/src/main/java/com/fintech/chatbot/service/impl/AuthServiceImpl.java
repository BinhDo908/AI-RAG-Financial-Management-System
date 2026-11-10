package com.fintech.chatbot.service.impl;

import com.fintech.chatbot.common.exception.BusinessException;
import com.fintech.chatbot.common.exception.ErrorCode;
import com.fintech.chatbot.config.properties.SecurityProperties;
import com.fintech.chatbot.dao.AiDataPermissionDao;
import com.fintech.chatbot.dao.UserDao;
import com.fintech.chatbot.dao.WalletDao;
import com.fintech.chatbot.dto.request.LoginRequest;
import com.fintech.chatbot.dto.request.RegisterRequest;
import com.fintech.chatbot.dto.response.AuthResponse;
import com.fintech.chatbot.entity.AiDataPermission;
import com.fintech.chatbot.entity.User;
import com.fintech.chatbot.entity.Wallet;
import com.fintech.chatbot.entity.enums.Role;
import com.fintech.chatbot.entity.enums.UserStatus;
import com.fintech.chatbot.entity.enums.WalletType;
import com.fintech.chatbot.mapper.UserMapper;
import com.fintech.chatbot.security.JwtService;
import com.fintech.chatbot.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    static final String DEFAULT_WALLET_NAME = "Tiền mặt";

    private final UserDao userDao;
    private final WalletDao walletDao;
    private final AiDataPermissionDao aiDataPermissionDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userDao.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(LocalDateTime.now(clock));
        userDao.save(user);

        // UC-D01 POST-2: ví "Tiền mặt" số dư 0, đặt làm ví mặc định
        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setName(DEFAULT_WALLET_NAME);
        wallet.setType(WalletType.CASH);
        wallet.setInitialBalance(BigDecimal.ZERO);
        wallet.setCurrentBalance(BigDecimal.ZERO);
        wallet.setDefaultWallet(true);
        walletDao.save(wallet);

        // UC-D05: mặc định cho phép AI dùng mọi nhóm dữ liệu
        AiDataPermission permission = new AiDataPermission();
        permission.setUserId(user.getId());
        aiDataPermissionDao.save(permission);

        return issueToken(user);
    }

    /**
     * Lỗi nghiệp vụ không rollback để bộ đếm đăng nhập sai vẫn được lưu (UC-D02 EX-2).
     */
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse login(LoginRequest request) {
        User user = userDao.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        LocalDateTime now = LocalDateTime.now(clock);

        if (user.getLoginLockedUntil() != null && user.getLoginLockedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.LOGIN_TEMPORARILY_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedLogin(user, now);
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        user.setFailedLoginCount(0);
        user.setLoginLockedUntil(null);
        user.setLastLoginAt(now);
        return issueToken(user);
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        userDao.findById(userId).ifPresent(User::bumpTokenVersion);
    }

    private void registerFailedLogin(User user, LocalDateTime now) {
        int failed = user.getFailedLoginCount() + 1;
        if (failed >= securityProperties.maxFailedLogins()) {
            user.setFailedLoginCount(0);
            user.setLoginLockedUntil(now.plusMinutes(securityProperties.loginLockMinutes()));
            throw new BusinessException(ErrorCode.LOGIN_TEMPORARILY_LOCKED);
        }
        user.setFailedLoginCount(failed);
        throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), userMapper.toResponse(user));
    }
}
