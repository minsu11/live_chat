package com.chat_server.security.filter;

import com.chat_server.security.provider.RsaKeyProvider;
import com.chat_server.user.dto.response.AuthorizationUserResponse;
import com.chat_server.user.service.AuthorizationService;
import com.chat_server.user.service.UserService;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

                AuthorizationUserResponse authorizationUserResponse = authorizationService.getAuthorizationUserByUserId(userId);
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authorizationUserResponse.role()));

                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                if(SecurityContextHolder.getContext().getAuthentication() == null) {

                }
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (ExpiredJwtException e) {
                // 만료시간

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

        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(c->"access_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setAuthenticationIfRequired(Long userId, List<SimpleGrantedAuthority> authorities) {
        var context =SecurityContextHolder.getContext();
        var existingAuth = context.getAuthentication();

        if (existingAuth == null || !existingAuth.isAuthenticated() || !existingAuth.getName().equals(userId)) {
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            context.setAuthentication(authentication);
        } else {
            log.debug("SecurityContext already contains authentication for userId: {}", userId);
        }
    }

}