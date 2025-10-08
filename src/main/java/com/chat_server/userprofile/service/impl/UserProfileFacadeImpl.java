package com.chat_server.userprofile.service.impl;


import com.chat_server.file.FileService;
import com.chat_server.user.service.UserService;
import com.chat_server.userprofile.dto.request.UserProfileUpdateRequest;
import com.chat_server.userprofile.service.UserProfileFacade;
import com.chat_server.userprofile.service.UserProfileService;
import java.util.Objects;
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
    @Override
    public void updateMyProfile(Long userId, UserProfileUpdateRequest request, MultipartFile file) {
        log.info("update profile service");
        String name = request.name();
        String message = request.message();

        if(name != null){
            // user nickname update
            userService.updateNickname(userId,name);
        }

        if(message != null){
            // user state message update
            userProfileService.updateStateMessage(userId,message);
        }

        if(file != null && !file.isEmpty()){

            String newUrl = fileService.saveProfileImage(userId,file);

        }
        log.info("update profile service complete");
    }
}
