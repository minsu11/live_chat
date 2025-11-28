package com.chat_server.user.service.impl;

import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.service.UserFacadeService;
import com.chat_server.user.service.UserService;
import com.chat_server.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserFacadeServiceImpl implements UserFacadeService {

    private final UserService userService;

    private final UserProfileService userProfileService;

    @Override
    public void signUp(UserRegisterRequest request) {
        log.info("UserFacadeServiceImpl signUp request received");

        String userUuid = userService.createUSer(request);

        userProfileService.createUserProfile(userUuid);
        log.info("UserFacadeServiceImpl signUp completed");

    }
}
