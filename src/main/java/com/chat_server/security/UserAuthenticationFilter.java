package com.chat_server.security;

import com.chat_server.user.dto.request.LoginRequest;
import com.chat_server.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * packageName    : com.chat_server.security
 * fileName       : Authentication
 * author         : parkminsu
 * date           : 25. 2. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 26.        parkminsu       최초 생성
 */
@Slf4j
public class UserAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final PasswordEncoder passwordEncoder;
    private final String ID_PARAMETER = "userId";
    private final String PW_PARAMETER = "password";
    public UserAuthenticationFilter(PasswordEncoder passwordEncoder) {
        this.setFilterProcessesUrl("/login");
        this.setUsernameParameter(ID_PARAMETER);
        this.setPasswordParameter(PW_PARAMETER);
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        log.info("attemptAuthentication 시작");
        ObjectMapper mapper = new ObjectMapper();
        try {
            LoginRequest loginRequest = mapper.readValue(request.getInputStream(), LoginRequest.class);
            log.info("Attempting to authenticate user: " + loginRequest.getUserId());
            log.info("Attempting to authenticate password: " + loginRequest.getPassword());
            return new UsernamePasswordAuthenticationToken(loginRequest.getUserId(), loginRequest.getPassword());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
