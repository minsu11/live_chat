package com.chat_server.user.service.impl;

import com.chat_server.security.dto.UserPrincipal;
import com.chat_server.gender.entity.Gender;
import com.chat_server.gender.exception.GenderNotFoundException;
import com.chat_server.gender.repository.GenderRepository;
import com.chat_server.user.domain.UserType;
import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.dto.response.UserAuthenticationResponse;
import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserAleadyExistException;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.user.service.UserService;
import com.chat_server.userstatus.entity.UserStatus;
import com.chat_server.userstatus.exception.UserStatusNotFoundException;
import com.chat_server.userstatus.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserStatusRepository userStatusRepository;
    private final GenderRepository genderRepository;


    @Override
    public void signUp(UserRegisterRequest registerRequest) {
        String id = registerRequest.id();
        log.debug("service start");
        if (userRepository.existsByUserInputId(id)) {
            throw new UserAleadyExistException("이미 존재하는 회원 입니다.");
        }
        
        // todo 회원가입 시 유저 프로필 생성하게 해야함, 디폴트 데이터를 yml 파일에 넣어서 관리할 예정
        UserStatus userStatus =
                userStatusRepository.findByUserStatusName("활성")
                        .orElseThrow(() -> new UserStatusNotFoundException("user status not found"));

        Gender gender = genderRepository.findByGenderName(registerRequest.gender())
                .orElseThrow(() -> new GenderNotFoundException("gender not found"));
        String password = passwordEncoder.encode(registerRequest.password());

        User user = User.builder()
                .userInputId(registerRequest.id())
                .userInputPassword(password)
                .userAge(registerRequest.age())
                .userName(registerRequest.name())
                .userNickname(registerRequest.nickName())
                .userStatus(userStatus)
                .gender(gender)
                .userCreatedAt(LocalDateTime.now())
                .userUuid(UUID.randomUUID().toString())
                .build();

        userRepository.save(user);
    }

    @Override
    public UserPrincipal loadUserByUserId(String userId) {
        log.debug("User Service loadUserByUserId start");
        UserAuthenticationResponse response = userRepository.getUserByUserId(userId)
                .orElseThrow(UserNotFoundException::new);

        return new UserPrincipal(userId, UserType.USER, response.userStatus());

    }
}
