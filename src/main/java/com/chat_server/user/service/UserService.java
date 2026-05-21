package com.chat_server.user.service;

import com.chat_server.security.dto.UserPrincipal;
import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.dto.response.UserIdResponse;
import com.chat_server.user.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * packageName    : com.chat_server.user.service
 * fileName       : UserService
 * author         : parkminsu
 * date           : 25. 2. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 26.        parkminsu       최초 생성
 */
public interface UserService {

    String createUSer(UserRegisterRequest registerRequest);

    void updateNickname(Long userId, String name);

    List<User> getUserIdByUserUuids(List<String> uuids);

    Long getUserIdByUserUuid(String userUuid);

    String getUuidByUserId(Long userId);

    User getUserById(Long userId);

    boolean validateUniqueInputId(String inputId);
}
