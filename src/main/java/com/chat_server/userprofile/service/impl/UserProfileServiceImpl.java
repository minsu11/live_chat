package com.chat_server.userprofile.service.impl;

import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.userprofile.dto.request.UserProfileUpdateRequest;
import com.chat_server.userprofile.dto.response.UserMyProfileDetailResponse;
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


    // todo 본인의 프로필 상세 내용을 가지고 옴. 프로필 사진을 클릭 한 뒤 나오는 데이터 들
    @Override
    @Transactional(readOnly = true)
    public UserMyProfileSummaryResponse getMyProfileSummary(Long userId) {
        log.info("getMyProfile");
        log.info("userId: " + userId);
        return userProfileRepository.findMyProfile(userId).orElseThrow(()->new UserNotFoundException("유저 프로필을 찾을 수 없음"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserMyProfileDetailResponse getMyProfileDetail(Long userId) {
        log.info("getMyProfileDetail");
        UserMyProfileDetailResponse response = getProfileDetail(userId);
        log.info("response: {}", response.toString());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserMyProfileDetailResponse getMyProfileDetail(String userId) {
        // 실제 친구가 있는지 확인
        log.info("getMyProfileDetail");
        Long id = userRepository.getUserIdByUserUuid(userId).orElseThrow(UserNotFoundException::new);
        log.info("id: {}", id);
        UserMyProfileDetailResponse response = getProfileDetail(id);
        log.info("response: {}", response.toString());

        return response;
    }

    @Override
    public void updateStateMessage(Long userId, String message) {
        log.info("updateUserProfile");

        UserProfile userProfile = userProfileRepository.findByUser_Id(userId)
            .orElseThrow(UserNotFoundException::new);

        userProfile.update(message);
    }

    @Override
    public void createUserProfile(String userUuid) {
        log.debug("Creating user profile url");
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(UserNotFoundException::new);

        UserProfile userProfile = UserProfile.builder()
                .stateMessage("")
                .user(user)
                .build();

        userProfileRepository.save(userProfile);

        log.debug("Creating user profile url end");
    }

    private UserMyProfileDetailResponse getProfileDetail(Long userId) {
        log.info("private method getProfileDetail");
        return userProfileRepository.findProfileDetail(userId)
                .orElse(new UserMyProfileDetailResponse("","","","",""));

    }

}
