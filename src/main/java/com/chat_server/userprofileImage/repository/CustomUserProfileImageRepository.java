package com.chat_server.userprofileImage.repository;

import com.chat_server.userprofileImage.entity.UserProfileImage;
import com.chat_server.userprofileImage.entity.UserProfileUrl;
import java.util.Optional;

public interface CustomUserProfileImageRepository {
    Optional<UserProfileImage> getUserProfileUrl(Long userId);
}
