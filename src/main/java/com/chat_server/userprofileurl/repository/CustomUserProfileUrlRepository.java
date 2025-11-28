package com.chat_server.userprofileurl.repository;

import com.chat_server.userprofileurl.entity.UserProfileUrl;
import java.util.Optional;

public interface CustomUserProfileUrlRepository {
    Optional<UserProfileUrl> getUserProfileUrl(Long userId);
}
