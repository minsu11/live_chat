package com.chat_server.user.service.impl;

import com.chat_server.user.domain.Role;
import com.chat_server.user.dto.response.AuthorizationUserResponse;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.user.service.AuthorizationService;
import com.chat_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {
    private final UserRepository userRepository;

    // user 권한
    @Override
    @Transactional(readOnly = true)
    public AuthorizationUserResponse getAuthorizationUserByUserId(String userId) {
        return userRepository.authorizeUserByUserId(userId, Role.유저.getValue())
                    .or(() -> userRepository.authorizeUserByUserId(userId, Role.관리자.getValue()))
                .orElseThrow(UserNotFoundException::new);
    }
}
