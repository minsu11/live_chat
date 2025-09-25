package com.chat_server.userprofile.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.userprofile.dto.response.UserMyProfileResponse;
import com.chat_server.userprofile.enrtity.UserProfile;
import com.chat_server.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${custom.api.common.prefix}${custom.api.user.profile.prefix}")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping()
    public ResponseEntity<ApiResponse<UserMyProfileResponse>> getMyProfile(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        log.info("내 프로필 가지고 오기");
        log.info("user id: {}", authenticatedUser.userId());
        Long userId = authenticatedUser.userId();
        UserMyProfileResponse userMyProfileResponse = userProfileService.getMyProfile(userId);

        ApiResponse<UserMyProfileResponse> response = ApiResponse.success(200,"본인 프로필을 불러왔습니다.", userMyProfileResponse);

        return ResponseEntity.ok(response);
    }
}
