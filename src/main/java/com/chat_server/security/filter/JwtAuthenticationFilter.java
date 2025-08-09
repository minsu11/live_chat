package com.chat_server.security.filter;

import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.security.provider.JwtTokenProvider;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.user.service.AuthorizationService;
import com.chat_server.util.CookieUtil;
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
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthorizationService authorizationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        log.debug("http 인가 처리");

        String token = CookieUtil.getCookie(request,"accessToken");

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String userId = jwtTokenProvider.getUserId(token);

            AuthenticatedUser authenticatedUser =
                    authorizationService.getAuthorizationUserByUserId(userId);

            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(authenticatedUser.role()));

            setAuthenticationIfRequired(authenticatedUser, authorities);
        } else {
            request.setAttribute("exception", ErrorCode.INVALID_TOKEN);
        }

        filterChain.doFilter(request, response);
    }


    private void setAuthenticationIfRequired(AuthenticatedUser authenticatedUser,
                                             List<SimpleGrantedAuthority> authorities) {
        var context = SecurityContextHolder.getContext();
        var existingAuth = context.getAuthentication();

        if (existingAuth == null || !existingAuth.isAuthenticated()) {
            var authentication =
                    new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
            context.setAuthentication(authentication);
        }
    }
}
