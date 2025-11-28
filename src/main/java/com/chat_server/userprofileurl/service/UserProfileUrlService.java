package com.chat_server.userprofileurl.service;

public interface UserProfileUrlService {
    void updateUserProfileUrl(Long userId, String newUrl);

    void createUserProfileUrl(String userUuid);
}
