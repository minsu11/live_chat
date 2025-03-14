package com.chat_server.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * packageName    : com.chat_server.security.handler
 * fileName       : FailHandler
 * author         : parkminsu
 * date           : 25. 2. 28.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 28.        parkminsu       최초 생성
 */
@Slf4j
public class CustomFailHandler implements AuthenticationFailureHandler {
    // log 남기기, 그리고 계정 잠금
    // todo servicce 하나 만들어서, 로그인 횟수 별로 잠금 기능 구현하기
    // redis에 테이블 저장
    // todo redis map에 저장 해야하나?

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.info("Authentication failure handler");
        log.info("실패");
        String username = request.getParameter("userId");


    }

}
