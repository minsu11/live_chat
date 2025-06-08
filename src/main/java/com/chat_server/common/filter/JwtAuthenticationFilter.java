//package com.chat_server.common.filter;
//
//import com.chat_server.authentication.dto.UserPrincipal;
//import com.chat_server.authentication.verifier.JwtVerifier;
//import com.chat_server.user.service.UserService;
//import com.chat_server.util.CookieUtil;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.ExpiredJwtException;
//import io.jsonwebtoken.JwtException;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
/// **
// * packageName    : com.chat_server.common.filter
// * fileName       : JwtAuthFilter
// * author         : parkminsu
// * date           : 25. 5. 21.
// * description    :
// * ===========================================================
// * DATE              AUTHOR             NOTE
// * -----------------------------------------------------------
// * 25. 5. 21.        parkminsu       최초 생성
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    private final JwtVerifier jwtVerifier;
//
//    private final UserService userService;
//
//    // jwt token 검증
//    // 토큰에 대한 기본적인 검증
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        // token
//        String accessToken = CookieUtil.getCookie(request, "access_token");
//
//        // token data null
//        if (accessToken == null || accessToken.isEmpty()) {
//            throw new ServletException("Missing access token");
//        }
//
//        try {
//            Claims claims = jwtVerifier.parseClaims(accessToken);
//            // principal 가지고옴
//            UserPrincipal userPrincipal = userService.loadUserByUserId(claims.getId());
//
//            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
//
//            // context holder에 저장
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//
//
//        } catch (ExpiredJwtException expiredJwtException) {
//            // 만료된 토큰은 front서버에서 응답
//            log.debug("Expired JWT token");
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Token Expired");
//            SecurityContextHolder.clearContext();
//        } catch (JwtException ignored) {
//            // 여기서 token 검증
//            log.error("Invalid JWT token");
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰 들어옴");
//            SecurityContextHolder.clearContext();
//        }
//
//        filterChain.doFilter(request, response);
//
//
//    }
//}
