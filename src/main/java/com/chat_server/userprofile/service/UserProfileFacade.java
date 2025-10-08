package com.chat_server.userprofile.service;

import com.chat_server.userprofile.dto.request.UserProfileUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileFacade {
    void updateMyProfile(Long userId, UserProfileUpdateRequest request, MultipartFile file);

}
