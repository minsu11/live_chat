package com.chat_server.userprofileImage.service;

import java.util.Optional;

public interface UserProfileImageService {
    void updateUserProfileUrl(Long userId, String newUrl);

    String getUserProfileUrl(Long userId);

}
