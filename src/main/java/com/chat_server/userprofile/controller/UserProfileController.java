package com.chat_server.userprofile.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.userprofile.dto.response.UserMyProfileInfoResponse;
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
@RequestMapping("${custom.api.common.prefix}${custom.api.user.prefix}")
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 로그인한 유저의 본인 프로필 모든 정보
     * @param authenticatedUser
     * @return
     */
    @GetMapping("me/profile")
    public ResponseEntity<ApiResponse<UserMyProfileInfoResponse>> getMyProfileDetail(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ){
        log.info("getMyProfileDetail");

        Long userId = authenticatedUser.userId();
        UserMyProfileInfoResponse userProfileDetailResponse = userProfileService.getMyProfileDetail(userId);
        ApiResponse<UserMyProfileInfoResponse> response = ApiResponse.success(200,"profile 성공적 반환",userProfileDetailResponse);
        log.info("end");
        return ResponseEntity.ok(response);
    }


//    /**
//     * 다른 사람의 프로필 상세 정보
//     * @param userId
//     * @return
//     */
//    @GetMapping("{userId}/profile/detail")
//    public ResponseEntity<ApiResponse<UserProfileDetailResponse>> getMyProfileDetail(
//        @RequestParam(name = "userId") String userId
//    ){
//        log.info("getMyProfileDetail");
//
//
//        UserProfileDetailResponse userProfileDetailResponse = userProfileService.getMyProfileDetail()
//    }


}
