package com.smhrd.hometraining.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * [담당] JWT 발급(로그인 성공 시)과 검증(매 요청마다 JwtAuthenticationFilter가 사용).
 * [DB] 없음 — 토큰 자체에 userId를 담아서 상태 없이(stateless) 검증한다.
 * [주의] ⚠️ 서명 키(app.jwt.secret)가 바뀌면 이미 발급된 모든 토큰이 무효가 된다(전원 재로그인
 *        필요). 회원 정지(users.status=SUSPENDED)는 다음 로그인부터만 막고, 이미 발급된 토큰
 *        자체를 즉시 무효화하지는 않는다(이 프로젝트에 토큰 블랙리스트가 없음).
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenMinutes;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String createAccessToken(Long userId, String loginId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("loginId", loginId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Long getUserId(String token) {
        Claims claims = parse(token);
        return Long.valueOf(claims.getSubject());
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
