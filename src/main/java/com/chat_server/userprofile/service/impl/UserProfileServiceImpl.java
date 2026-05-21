package com.chat_server.userprofile.service.impl;

import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.userprofile.dto.response.UserProfileDetailResponse;
import com.chat_server.userprofile.dto.response.UserMyProfileSummaryResponse;
import com.chat_server.userprofile.enrtity.UserProfile;
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

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public UserMyProfileSummaryResponse getMyProfileSummary(Long userId) {
        log.info("getMyProfileSummary userId={}", userId);

        return userProfileRepository.findMyProfile(userId)
                .orElseThrow(() -> new UserNotFoundException("유저 프로필을 찾을 수 없습니다."));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDetailResponse getMyProfileDetail(Long userId) {
        log.info("getMyProfileDetail userId={}", userId);

        return userProfileRepository.findProfileDetail(userId)
                .orElseThrow(() -> new UserNotFoundException("유저 프로필을 찾을 수 없습니다."));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDetailResponse getProfileDetailByUuid(Long viewerId, String targetUserUuid) {
        log.info("getProfileDetailByUuid viewerId={}, targetUserUuid={}", viewerId, targetUserUuid);

        return userProfileRepository.findProfileDetailByUuid(viewerId, targetUserUuid)
                .orElseThrow(() -> new UserNotFoundException("유저 프로필을 찾을 수 없습니다."));
    }

    @Override
    public void updateStateMessage(Long userId, String message) {
        log.info("updateStateMessage userId={}", userId);

        UserProfile userProfile = userProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new UserNotFoundException("유저 프로필을 찾을 수 없습니다."));

        userProfile.update(message);
    }

    @Override
    public void createUserProfile(String userUuid) {
        log.debug("Creating user profile userUuid={}", userUuid);

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));

        UserProfile userProfile = UserProfile.builder()
                .stateMessage("")
                .user(user)
                .build();

        userProfileRepository.save(userProfile);

        log.debug("Creating user profile end");
    }
}