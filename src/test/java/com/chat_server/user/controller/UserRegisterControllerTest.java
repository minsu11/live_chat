package com.chat_server.user.controller;

import com.chat_server.user.dto.request.InputIdCheckRequest;
import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.dto.response.InputIdCheckResponse;
import com.chat_server.user.service.UserFacadeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserRegisterControllerTest {
    @Test
    @DisplayName("회원가입 성공 시 201 상태를 반환하고 Facade를 호출한다")
    void registerShouldReturnCreated() {
        UserFacadeService service = mock(UserFacadeService.class);
        UserRegisterController controller = new UserRegisterController(service);
        UserRegisterRequest request = new UserRegisterRequest("input01", "password", "홍길동", "길동", 20, "MALE", null, null);

        var response = controller.register(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getStatus()).isEqualTo(201);
        verify(service).signUp(request);
    }

    @Test
    @DisplayName("아이디 중복 확인 성공 시 200 상태와 가능 여부를 반환한다")
    void checkInputIdShouldReturnAvailability() {
        UserFacadeService service = mock(UserFacadeService.class);
        UserRegisterController controller = new UserRegisterController(service);
        InputIdCheckRequest request = new InputIdCheckRequest("input01");
        when(service.checkInputIdAvailability(request)).thenReturn(new InputIdCheckResponse(true));

        var response = controller.checkInputId(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().available()).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 시 Facade 예외를 전파한다")
    void registerShouldPropagateFacadeException() {
        UserFacadeService service = mock(UserFacadeService.class);
        UserRegisterController controller = new UserRegisterController(service);
        UserRegisterRequest request = new UserRegisterRequest("input01", "password", "홍길동", "길동", 20, "MALE", null, null);
        doThrow(new IllegalStateException("duplicated")).when(service).signUp(request);

        assertThatThrownBy(() -> controller.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("duplicated");
    }
}
