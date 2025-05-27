package com.chat_server.common.filter;

import com.chat_server.authentication.dto.UserPrincipal;
import com.chat_server.authentication.verifier.JwtVerifier;
import com.chat_server.user.service.UserService;
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

    private final UserService userService;


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
            // principal 가지고옴
            UserPrincipal userPrincipal = userService.loadUserByUserId(claims.getId());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, null);

            // context holder에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException ignored) {
            // 여기서 token 검증
            

        }

    }
}
