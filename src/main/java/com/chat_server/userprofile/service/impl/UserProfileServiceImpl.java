package com.chat_server.userprofile.service.impl;

import com.chat_server.userprofile.dto.response.UserMyProfileResponse;
import com.chat_server.userprofile.repository.UserProfileRepository;
import com.chat_server.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileRepository userProfileRepository;

    @Override
    public UserMyProfileResponse getMyProfile(Long userId) {
        log.info("getMyProfile");



        return null;
    }
}
