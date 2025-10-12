package com.chat_server.userprofile.service;

import com.chat_server.userprofile.dto.request.UserProfileUpdateRequest;
import com.chat_server.userprofile.dto.response.UserProfileUpdateResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileFacade {
    UserProfileUpdateResponse updateMyProfile(Long userId, UserProfileUpdateRequest request, MultipartFile file);

}
