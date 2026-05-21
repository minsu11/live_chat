package com.chat_server.userprofile.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.userprofile.dto.request.UserProfileUpdateRequest;
import com.chat_server.userprofile.dto.response.UserProfileDetailResponse;
import com.chat_server.userprofile.dto.response.UserMyProfileSummaryResponse;
import com.chat_server.userprofile.dto.response.UserProfileUpdateImageResponse;
import com.chat_server.userprofile.dto.response.UserProfileUpdateResponse;
import com.chat_server.userprofile.service.UserProfileFacade;
import com.chat_server.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${custom.api.common.prefix}${custom.api.user.prefix}")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfileFacade userProfileFacade;

    /**
     * 로그인한 유저의 본인 프로필 요약 정보
     */
    @GetMapping("/me/profile/summary")
    public ResponseEntity<ApiResponse<UserMyProfileSummaryResponse>> getMyProfileSummary(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        log.info("getMyProfileSummary");

        Long userId = authenticatedUser.userId();
        UserMyProfileSummaryResponse responseData = userProfileService.getMyProfileSummary(userId);

        return ResponseEntity.ok(
                ApiResponse.success(200, "profile 성공적 반환", responseData)
        );
    }

    /**
     * 로그인한 유저의 본인 프로필 상세 정보
     */
    @GetMapping("/me/profile/detail")
    public ResponseEntity<ApiResponse<UserProfileDetailResponse>> getMyProfileDetail(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        log.info("getMyProfileDetail");

        Long userId = authenticatedUser.userId();
        UserProfileDetailResponse responseData = userProfileService.getMyProfileDetail(userId);

        return ResponseEntity.ok(
                ApiResponse.success(200, "profile 성공적 반환", responseData)
        );
    }

    /**
     * uuid 기반 프로필 상세 정보 조회
     * - 친구 목록 프로필 클릭
     * - 채팅방 메시지 프로필 클릭
     * - 검색 결과 프로필 클릭
     */
    @GetMapping("/profile/{userUuid}")
    public ResponseEntity<ApiResponse<UserProfileDetailResponse>> getProfileDetailByUuid(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable String userUuid
    ) {
        log.info("getProfileDetailByUuid userUuid={}", userUuid);

        Long viewerId = authenticatedUser.userId();

        UserProfileDetailResponse responseData =
                userProfileService.getProfileDetailByUuid(viewerId, userUuid);

        return ResponseEntity.ok(
                ApiResponse.success(200, "프로필 상세 정보 반환 완료", responseData)
        );
    }

    @PostMapping(
            value = "/me/profile/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<UserProfileUpdateImageResponse>> updateMyProfileImage(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestPart(value = "file") MultipartFile file
    ) {
        log.info("updateMyProfileImage");

        Long userId = authenticatedUser.userId();
        UserProfileUpdateImageResponse responseData =
                userProfileFacade.updateMyProfileImage(userId, file);

        return ResponseEntity.ok(
                ApiResponse.success(201, "update 완료", responseData)
        );
    }

    @PostMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileUpdateResponse>> updateMyProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody UserProfileUpdateRequest request
    ) {
        log.info("updateMyProfile");

        Long userId = authenticatedUser.userId();
        UserProfileUpdateResponse responseData =
                userProfileFacade.updateMyProfile(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(200, "수정 완료", responseData)
        );
    }
}