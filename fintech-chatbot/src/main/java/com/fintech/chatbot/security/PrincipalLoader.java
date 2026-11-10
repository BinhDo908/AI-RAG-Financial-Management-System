package com.fintech.chatbot.security;

import com.fintech.chatbot.dao.UserDao;
import com.fintech.chatbot.entity.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Nạp người dùng từ token. Token chỉ hợp lệ khi tài khoản còn ACTIVE
 * và tokenVersion trong token khớp với DB (đăng xuất / đổi mật khẩu làm token cũ vô hiệu).
 */
@Service
@RequiredArgsConstructor
public class PrincipalLoader {

    private final UserDao userDao;

    @Transactional(readOnly = true)
    public Optional<UserPrincipal> load(JwtService.TokenClaims claims) {
        return userDao.findById(claims.userId())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> u.getTokenVersion() == claims.tokenVersion())
                .map(u -> new UserPrincipal(u.getId(), u.getEmail(), u.getRole()));
    }
}
