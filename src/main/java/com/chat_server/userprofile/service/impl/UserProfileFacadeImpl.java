package com.chat_server.userprofile.service.impl;


import com.chat_server.file.service.FileService;
import com.chat_server.user.service.UserService;
import com.chat_server.userprofile.dto.request.UserProfileUpdateRequest;
import com.chat_server.userprofile.dto.response.UserProfileUpdateImageResponse;
import com.chat_server.userprofile.dto.response.UserProfileUpdateResponse;
import com.chat_server.userprofile.service.UserProfileFacade;
import com.chat_server.userprofile.service.UserProfileService;
import com.chat_server.userprofileImage.service.UserProfileImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileFacadeImpl implements UserProfileFacade {
    private final FileService fileService;
    private final UserService userService;
    private final UserProfileService userProfileService;
    private final UserProfileImageService userProfileUrlService;

    @Override
    public UserProfileUpdateResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        log.info("update profile service");
        String name = request.name();
        String message = request.message();
        String newUrl = null;
        if(name != null){
            // user nickname update
            userService.updateNickname(userId,name);
        }

        if(message != null){
            // user state message update
            userProfileService.updateStateMessage(userId,message);
        }

        log.info("update profile service complete");

        return new UserProfileUpdateResponse(name,message);
    }

    @Override
    public UserProfileUpdateImageResponse updateMyProfileImage(Long userId, MultipartFile file) {

        String newUrl = fileService.saveProfileImage(userId,file);
        log.info("new profile url: {}", newUrl);
        userProfileUrlService.updateUserProfileUrl(userId,newUrl);
        return new UserProfileUpdateImageResponse(newUrl);
    }
}
