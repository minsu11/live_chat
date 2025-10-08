package com.chat_server.userprofile.url.service.impl;

import com.chat_server.userprofile.url.entity.UserProfileUrl;
import com.chat_server.userprofile.url.repository.UserProfileUrlRepository;
import com.chat_server.userprofile.url.service.UserProfileUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileUrlServiceImpl implements UserProfileUrlService {
    private final UserProfileUrlRepository userProfileUrlRepository;

    // update 시 기존 active 프로필 false 바꾸고, 새로운 profile url 추가
    @Override
    public void updateUserProfileUrl(Long userId, String newUrl) {
        log.info("Updating user profile url");
        UserProfileUrl oldUserProfileUrl = userProfileUrlRepository.getUserProfileUrl(userId)
            .orElse(null);
        if(oldUserProfileUrl != null) {
            oldUserProfileUrl.updateIsCurrent(false);
        }

        //
        UserProfileUrl newUserProfileUrl = UserProfileUrl.builder()
            .imageUrl(newUrl)
            .build();

    }
}
