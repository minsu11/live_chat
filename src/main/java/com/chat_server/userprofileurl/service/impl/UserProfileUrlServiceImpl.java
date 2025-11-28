package com.chat_server.userprofileurl.service.impl;

import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.userprofile.enrtity.UserProfile;
import com.chat_server.userprofile.exception.UserProfileNotFoundException;
import com.chat_server.userprofile.repository.UserProfileRepository;
import com.chat_server.userprofileurl.entity.UserProfileUrl;
import com.chat_server.userprofileurl.repository.UserProfileUrlRepository;
import com.chat_server.userprofileurl.service.UserProfileUrlService;
import java.time.LocalDateTime;
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
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    // update 시 기존 active 프로필 false 바꾸고, 새로운 profile url 추가
    @Override
    public void updateUserProfileUrl(Long userId, String newUrl) {
        log.info("Updating user profile url");
        UserProfileUrl oldUserProfileUrl = userProfileUrlRepository.getUserProfileUrl(userId)
            .orElse(null);

        if(oldUserProfileUrl != null) {
            oldUserProfileUrl.updateIsCurrent(false);
        }

        UserProfile userProfile = userProfileRepository.findByUser_Id(userId)
            .orElseThrow(UserProfileNotFoundException::new);

        UserProfileUrl newUserProfileUrl = UserProfileUrl.builder()
            .imageUrl(newUrl)
            .isCurrent(true)
            .uploadedAt(LocalDateTime.now())
            .userProfile(userProfile)
            .build();
        userProfileUrlRepository.save(newUserProfileUrl);
        log.info("url 저장 완료");
    }

}
