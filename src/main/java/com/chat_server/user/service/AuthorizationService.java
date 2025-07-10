package com.chat_server.user.service;

import com.chat_server.user.dto.response.AuthenticatedUser;

public interface AuthorizationService {
    AuthenticatedUser getAuthorizationUserByUserId(String userId);
}
