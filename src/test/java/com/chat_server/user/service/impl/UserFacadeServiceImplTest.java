package com.chat_server.user.service.impl;

import com.chat_server.user.dto.request.InputIdCheckRequest;
import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.service.UserService;
import com.chat_server.userprofile.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserFacadeServiceImplTest {
    @Test
    @DisplayName("회원가입 Facade 성공 시 사용자 생성 후 프로필을 생성한다")
    void signUpShouldCreateUserAndProfile() {
        UserService userService = mock(UserService.class);
        UserProfileService profileService = mock(UserProfileService.class);
        UserFacadeServiceImpl facade = new UserFacadeServiceImpl(userService, profileService);
        UserRegisterRequest request = new UserRegisterRequest("input01", "password", "홍길동", "길동", 20, "MALE", null, null);
        when(userService.createUSer(request)).thenReturn("uuid");

        facade.signUp(request);

        verify(userService).createUSer(request);
        verify(profileService).createUserProfile("uuid");
    }

    @Test
    @DisplayName("아이디 중복 확인 성공 시 UserService 결과를 응답으로 감싼다")
    void checkInputIdAvailabilityShouldReturnUserServiceResult() {
        UserService userService = mock(UserService.class);
        UserProfileService profileService = mock(UserProfileService.class);
        UserFacadeServiceImpl facade = new UserFacadeServiceImpl(userService, profileService);
        when(userService.validateUniqueInputId("new_id")).thenReturn(true);

        assertThat(facade.checkInputIdAvailability(new InputIdCheckRequest("new_id")).available()).isTrue();
    }
}
