package com.chat_server.userprofile.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.userprofile.dto.response.UserMyProfileDetailResponse;
import com.chat_server.userprofile.dto.response.UserMyProfileSummaryResponse;
import com.chat_server.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping("me/profile/summary")
    public ResponseEntity<ApiResponse<UserMyProfileSummaryResponse>> getMyProfileSummary(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ){
        log.info("getMyProfileSummary");
        Long userId = authenticatedUser.userId();
        UserMyProfileSummaryResponse userProfileDetailResponse = userProfileService.getMyProfileSummary(userId);
        ApiResponse<UserMyProfileSummaryResponse> response = ApiResponse.success(200,"profile 성공적 반환",userProfileDetailResponse);
        log.info("end");
        return ResponseEntity.ok(response);
    }



    /**
     * 로그인한 유저의 본인 프로필 모든 정보
     * @param authenticatedUser
     * @return
     */
    @GetMapping("me/profile/detail")
    public ResponseEntity<ApiResponse<UserMyProfileDetailResponse>> getMyProfileDetail(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ){
        log.info("getMyProfileDetail");

        Long userId = authenticatedUser.userId();
        UserMyProfileDetailResponse userProfileDetailResponse = userProfileService.getMyProfileDetail(userId);
        ApiResponse<UserMyProfileDetailResponse> response = ApiResponse.success(200,"profile 성공적 반환",userProfileDetailResponse);
        log.info("end");
        return ResponseEntity.ok(response);
    }


    /**
     * 다른 사람의 프로필 상세 정보
     * @param userId user 식별키(uuid)
     * @return
     */
    @GetMapping("{userId}/profile/detail")
    public ResponseEntity<ApiResponse<UserMyProfileDetailResponse>> getMyProfileDetail(
            @PathVariable(name = "userId")  String userId
    ){
        log.info("getMyProfileDetail");
        log.info("userId: " + userId);

        UserMyProfileDetailResponse userProfileDetailResponse = userProfileService.getMyProfileDetail(userId);
        ApiResponse<UserMyProfileDetailResponse> response = ApiResponse.success(200,"다른 사람 프로필 상세 정보 반환 완료", userProfileDetailResponse);

        log.info("end");
        return ResponseEntity.ok(response);
    }


}
