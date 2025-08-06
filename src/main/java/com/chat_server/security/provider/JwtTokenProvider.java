package com.chat_server.security.provider;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.PublicKey;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final RsaKeyProvider rsaKeyProvider;

    // Claims 추출
    public Claims parseClaims(String token) {
        PublicKey publicKey = rsaKeyProvider.getPublicKey();
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("토큰 만료", e);
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("토큰 유효성 오류", e);
            return false;
        }
    }

    // userId 추출
    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }
}

