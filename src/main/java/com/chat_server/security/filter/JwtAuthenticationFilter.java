package com.chat_server.security.filter;

import com.chat_server.security.provider.RsaKeyProvider;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.user.service.AuthorizationService;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RsaKeyProvider rsaKeyProvider;
    private final AuthorizationService authorizationService;


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

                AuthenticatedUser authenticatedUser = authorizationService.getAuthorizationUserByUserId(userId);
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authenticatedUser.role()));

                setAuthenticationIfRequired(authenticatedUser,authorities);
            } catch (ExpiredJwtException e) {
                log.warn("토큰 만료", e);
                //TODO#1 Redis에서 Refresh Token 조회 및 재발급 처리
                request.setAttribute("exception", "TOKEN_EXPIRED");
                // 
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

        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(c->"access_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setAuthenticationIfRequired(AuthenticatedUser authenticatedUser, List<SimpleGrantedAuthority> authorities) {
        var context = SecurityContextHolder.getContext();
        // authentication
        var existingAuth = context.getAuthentication();

        if (existingAuth == null || !existingAuth.isAuthenticated() ) {
            var authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
            context.setAuthentication(authentication);
        } else {
            log.debug("SecurityContext already contains authentication for userId: {}", authenticatedUser);
        }
    }

}