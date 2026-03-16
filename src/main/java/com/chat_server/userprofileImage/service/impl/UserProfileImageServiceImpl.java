package com.chat_server.userprofileImage.service.impl;

import com.chat_server.user.repository.UserRepository;
import com.chat_server.userprofile.enrtity.UserProfile;
import com.chat_server.userprofile.exception.UserProfileNotFoundException;
import com.chat_server.userprofile.repository.UserProfileRepository;
import com.chat_server.userprofileImage.entity.UserProfileImage;
import com.chat_server.userprofileImage.repository.UserProfileImageRepository;
import com.chat_server.userprofileImage.service.UserProfileImageService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileImageServiceImpl implements UserProfileImageService {
    private final UserProfileImageRepository userProfileUrlRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    // update 시 기존 active 프로필 false 바꾸고, 새로운 profile url 추가
    @Override
    public void updateUserProfileUrl(Long userId, String newUrl) {
        log.info("Updating user profile url");
        UserProfileImage oldUserProfileUrl = userProfileUrlRepository.getUserProfileUrl(userId)
            .orElse(null);

        if(oldUserProfileUrl != null) {
            oldUserProfileUrl.updateCurrent(false);
        }

        UserProfile userProfile = userProfileRepository.findByUser_Id(userId)
            .orElseThrow(UserProfileNotFoundException::new);

        UserProfileImage newUserProfileUrl = UserProfileImage.builder()
            .imageUrl(newUrl)
            .current(true)
            .uploadedAt(LocalDateTime.now())
            .userProfile(userProfile)
            .build();
        userProfileUrlRepository.save(newUserProfileUrl);
        log.info("url 저장 완료");
    }

}
