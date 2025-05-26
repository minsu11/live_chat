package com.chat_server.common.filter;

import com.chat_server.authentication.verifier.JwtVerifier;
import com.chat_server.util.CookieUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * packageName    : com.chat_server.common.filter
 * fileName       : JwtAuthFilter
 * author         : parkminsu
 * date           : 25. 5. 21.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 21.        parkminsu       최초 생성
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtVerifier jwtVerifier;

    // redis service
    // jwt token 검증
    // 토큰에 대한 기본적인 검증
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // token 
        String accessToken = CookieUtil.getCookie(request, "access_token");

        // token data null
        if (accessToken == null || accessToken.isEmpty()) {
            throw new ServletException("Missing access token");
        }

        try {
            Claims claims = jwtVerifier.parseClaims(accessToken);
            //
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(claims.getId(), null, null);

            SecurityContextHolder.getContext().setAuthentication();

        } catch (JwtException ignored) {
            // 여기서 token 검증
            

        }

    }
}
