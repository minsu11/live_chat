package com.chat_server.user.adaptor;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.request.LoginRequest;
import com.chat_server.user.dto.response.LoginTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * packageName    : com.chat_server.user.adaptor
 * fileName       : AuthServerAdaptor
 * author         : parkminsu
 * date           : 25. 5. 9.
 * description    : auth server 연결하는 부분
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 9.        parkminsu       최초 생성
 */

// context id는 해당 adaptor 고유 이름
@FeignClient(name = "auth-client", url = "${auth.server.url}", contextId = "auth-login-client", path = "${auth.api.address}")
public interface AuthServerAdaptor {
    // login 연결
    // 여기서 token의 데이터를 받아야함
    @PostMapping("/login")
    ApiResponse<LoginTokenResponse> login(LoginRequest loginRequest);

    // logout 기능도 추가 예정
}
