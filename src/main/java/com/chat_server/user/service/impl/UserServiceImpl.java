package com.chat_server.user.service.impl;

import com.chat_server.gender.entity.Gender;
import com.chat_server.gender.exception.GenderNotFoundException;
import com.chat_server.gender.repository.GenderRepository;
import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserAleadyExistException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.user.service.UserService;
import com.chat_server.userstatus.entity.UserStatus;
import com.chat_server.userstatus.exception.UserStatusNotFoundException;
import com.chat_server.userstatus.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

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
        String id = registerRequest.userId();
        log.debug("service start");
        if(userRepository.existsByUserInputId(id)){
            throw new UserAleadyExistException("이미 존재하는 회원 입니다.");
        }

        UserStatus userStatus =
                userStatusRepository.findByUserStatusName("활성")
                .orElseThrow(()-> new UserStatusNotFoundException("user status not found"));

        Gender gender = genderRepository.findByGenderName(registerRequest.gender())
                .orElseThrow(()-> new GenderNotFoundException("gender not found"));

        User user = User.builder()
                .userInputId(registerRequest.userId())
                .userInputPassword(registerRequest.password())
                .userAge(registerRequest.age())
                .userName(registerRequest.name())
                .userNickname(registerRequest.nickname())
                .userStatus(userStatus)
                .gender(gender)
                .userCreatedAt(LocalDateTime.now())

                .build();

        userRepository.save(user);
    }
}
