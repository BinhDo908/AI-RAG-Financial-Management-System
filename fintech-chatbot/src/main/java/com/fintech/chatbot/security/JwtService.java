package com.fintech.chatbot.security;

import com.fintech.chatbot.config.properties.JwtProperties;
import com.fintech.chatbot.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Tạo và kiểm tra JWT. Token mang claim {@code ver} = User.tokenVersion;
 * khi đăng xuất / đổi mật khẩu, tokenVersion tăng lên nên mọi token cũ mất hiệu lực.
 */
@Service
public class JwtService {

    private static final String CLAIM_VERSION = "ver";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(JwtProperties props) {
        if (props.secret() == null || props.secret().getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) phải có ít nhất 32 byte");
        }
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = props.expirationMinutes();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_VERSION, user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    /**
     * @return thông tin token nếu chữ ký và hạn còn hợp lệ.
     */
    public Optional<TokenClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            Integer version = claims.get(CLAIM_VERSION, Integer.class);
            return Optional.of(new TokenClaims(userId, version == null ? 0 : version));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record TokenClaims(Long userId, int tokenVersion) {
    }
}
