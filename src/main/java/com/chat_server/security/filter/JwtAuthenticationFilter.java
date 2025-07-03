package com.chat_server.security.filter;

import com.chat_server.security.provider.RsaKeyProvider;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RsaKeyProvider rsaKeyProvider;

    public JwtAuthenticationFilter(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractTokenFromCookie(request);

        if (token != null) {
            try {
                Claims claims = parseClaims(token);
                String userId = claims.getSubject();

                var authorities = extractAuthorities(claims);

                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (ExpiredJwtException e) {
                log.warn("토큰 만료", e);
                request.setAttribute("exception", "TOKEN_EXPIRED");
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("토큰 유효성 오류", e);
                request.setAttribute("exception", "INVALID_TOKEN");
            }
        }

        filterChain.doFilter(request, response);
    }

    private Claims parseClaims(String token) {
        PublicKey publicKey = rsaKeyProvider.getPublicKey();
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private List<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}