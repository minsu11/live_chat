package com.chat_server.userprofileImage.repository;

import com.chat_server.userprofileImage.entity.UserProfileImage;
import java.util.Optional;

public interface CustomUserProfileImageRepository {
    Optional<UserProfileImage> getUserProfileImage(Long userId);
    Optional<String> getUserProfileImageUrlByUserId(Long userId);
}
