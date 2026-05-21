package com.chat_server.user.service;

import com.chat_server.user.dto.request.InputIdCheckRequest;
import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.dto.response.InputIdCheckResponse;

public interface UserFacadeService  {
    void signUp(UserRegisterRequest request);

    InputIdCheckResponse checkInputIdAvailability(InputIdCheckRequest inputIdCheckRequest);
}
