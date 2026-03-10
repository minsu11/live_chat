package com.chat_server.user.service;

import com.chat_server.user.dto.request.UserRegisterRequest;

public interface UserFacadeService  {
    void signUp(UserRegisterRequest request);

}
