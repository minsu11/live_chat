package com.chat_server.user.service;

import com.chat_server.user.dto.response.AuthorizationUserResponse;

public interface AuthorizationService {
    AuthorizationUserResponse getAuthorizationUserByUserId(String userId);
}
