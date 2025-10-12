package com.chat_server.userprofile.url.repository;

import com.chat_server.userprofile.url.entity.UserProfileUrl;
import java.util.Optional;

public interface CustomUserProfileUrlRepository {
    Optional<UserProfileUrl> getUserProfileUrl(Long userId);
}
