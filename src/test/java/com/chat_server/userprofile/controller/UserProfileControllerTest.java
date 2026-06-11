package com.chat_server.userprofile.controller;

import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.userprofile.dto.request.UserProfileUpdateRequest;
import com.chat_server.userprofile.dto.response.UserMyProfileSummaryResponse;
import com.chat_server.userprofile.dto.response.UserProfileDetailResponse;
import com.chat_server.userprofile.dto.response.UserProfileUpdateImageResponse;
import com.chat_server.userprofile.dto.response.UserProfileUpdateResponse;
import com.chat_server.userprofile.service.UserProfileFacade;
import com.chat_server.userprofile.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserProfileControllerTest {
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "USER");

    @Test
    @DisplayName("내 프로필 요약 조회 성공 시 200 상태와 요약 정보를 반환한다")
    void getMyProfileSummaryShouldReturnSummary() {
        UserProfileService service = mock(UserProfileService.class);
        UserProfileFacade facade = mock(UserProfileFacade.class);
        UserProfileController controller = new UserProfileController(service, facade);
        UserMyProfileSummaryResponse data = new UserMyProfileSummaryResponse("u1", "me", "CTK-11111111", "hi", null);
        when(service.getMyProfileSummary(1L)).thenReturn(data);

        assertThat(controller.getMyProfileSummary(user).getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("내 프로필 상세 조회 성공 시 200 상태와 상세 정보를 반환한다")
    void getMyProfileDetailShouldReturnDetail() {
        UserProfileService service = mock(UserProfileService.class);
        UserProfileFacade facade = mock(UserProfileFacade.class);
        UserProfileController controller = new UserProfileController(service, facade);
        UserProfileDetailResponse data = new UserProfileDetailResponse("u1", "me", "CTK-11111111", "hi", null, false, true);
        when(service.getMyProfileDetail(1L)).thenReturn(data);

        assertThat(controller.getMyProfileDetail(user).getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("UUID 기반 프로필 상세 조회 성공 시 viewerId와 targetUuid를 서비스에 전달한다")
    void getProfileDetailByUuidShouldReturnDetail() {
        UserProfileService service = mock(UserProfileService.class);
        UserProfileFacade facade = mock(UserProfileFacade.class);
        UserProfileController controller = new UserProfileController(service, facade);
        UserProfileDetailResponse data = new UserProfileDetailResponse("u2", "friend", "CTK-22222222", "hello", null, true, false);
        when(service.getProfileDetailByUuid(1L, "u2")).thenReturn(data);

        assertThat(controller.getProfileDetailByUuid(user, "u2").getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("프로필 이미지 수정 성공 시 201 상태와 새 이미지 URL을 반환한다")
    void updateMyProfileImageShouldReturnImageResponse() {
        UserProfileService service = mock(UserProfileService.class);
        UserProfileFacade facade = mock(UserProfileFacade.class);
        UserProfileController controller = new UserProfileController(service, facade);
        MultipartFile file = new MockMultipartFile("file", "p.png", "image/png", "png".getBytes());
        UserProfileUpdateImageResponse data = new UserProfileUpdateImageResponse("/p.png");
        when(facade.updateMyProfileImage(1L, file)).thenReturn(data);

        var response = controller.updateMyProfileImage(user, file);

        assertThat(response.getBody().getStatus()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("프로필 수정 성공 시 200 상태와 수정 결과를 반환한다")
    void updateMyProfileShouldReturnUpdateResponse() {
        UserProfileService service = mock(UserProfileService.class);
        UserProfileFacade facade = mock(UserProfileFacade.class);
        UserProfileController controller = new UserProfileController(service, facade);
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("new", "message", null);
        UserProfileUpdateResponse data = new UserProfileUpdateResponse("new", "message");
        when(facade.updateMyProfile(1L, request)).thenReturn(data);

        assertThat(controller.updateMyProfile(user, request).getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("프로필 조회 실패 시 서비스 예외를 전파한다")
    void profileControllerShouldPropagateServiceException() {
        UserProfileService service = mock(UserProfileService.class);
        UserProfileFacade facade = mock(UserProfileFacade.class);
        UserProfileController controller = new UserProfileController(service, facade);
        when(service.getMyProfileSummary(1L)).thenThrow(new IllegalStateException("profile missing"));

        assertThatThrownBy(() -> controller.getMyProfileSummary(user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("profile missing");
    }
}
