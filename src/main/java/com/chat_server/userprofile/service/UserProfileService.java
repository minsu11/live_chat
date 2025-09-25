package com.chat_server.userprofile.service;

import com.chat_server.userprofile.dto.response.UserMyProfileResponse;

public interface UserProfileService {
    UserMyProfileResponse getMyProfile(Long userId);
}
