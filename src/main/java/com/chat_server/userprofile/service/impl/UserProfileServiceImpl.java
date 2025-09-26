package com.chat_server.userprofile.service.impl;

import com.chat_server.userprofile.dto.response.UserMyProfileResponse;
import com.chat_server.userprofile.repository.UserProfileRepository;
import com.chat_server.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileRepository userProfileRepository;

    // todo 본인의 프로필 상세 내용을 가지고 옴. 프로필 사진을 클릭 한 뒤 나오는 데이터 들
    @Override
    public UserMyProfileResponse getMyProfile(Long userId) {
        log.info("getMyProfile");


        return userProfileRepository.findMyProfile(userId).orElse(new UserMyProfileResponse("",""));
    }
}
