package com.chat_server.user.service.impl;

import com.chat_server.common.dto.exception.NotFoundException;
import com.chat_server.logintype.entity.LoginType;
import com.chat_server.logintype.enums.LoginTypeEnum;
import com.chat_server.logintype.repository.LoginTypeRepository;
import com.chat_server.security.dto.UserPrincipal;
import com.chat_server.gender.entity.Gender;
import com.chat_server.gender.exception.GenderNotFoundException;
import com.chat_server.gender.repository.GenderRepository;
import com.chat_server.user.enums.UserStatus;
import com.chat_server.user.enums.UserType;
import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.dto.response.UserAuthenticationResponse;
import com.chat_server.user.dto.response.UserIdResponse;
import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserAleadyExistException;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.user.service.UserService;
import com.chat_server.userprofile.enrtity.UserProfile;
import com.chat_server.userprofile.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
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
    private static final String FRIEND_CODE_PREFIX = "CTK-";
    private static final String FRIEND_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int FRIEND_CODE_RANDOM_LENGTH = 8;
    private static final int FRIEND_CODE_MAX_RETRY = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GenderRepository genderRepository;
    private final UserProfileRepository userProfileRepository;
    private final LoginTypeRepository loginTypeRepository;


    @Override
    public String createUSer(UserRegisterRequest registerRequest) {
        String id = registerRequest.id();
        log.debug("service start");
        if (userRepository.existsByInputId(id)) {
            throw new UserAleadyExistException("이미 존재하는 회원 입니다.");
        }

        Gender gender = genderRepository.findByName(registerRequest.gender())
                .orElseThrow(() -> new GenderNotFoundException("gender not found"));
        String password = passwordEncoder.encode(registerRequest.password());
        String userUuid = UUID.randomUUID().toString();
        LoginType loginType = loginTypeRepository.findByName(LoginTypeEnum.LOCAL.name())
                .orElseThrow(NotFoundException::new);

        User user = User.builder()
                .inputId(registerRequest.id())
                .inputPassword(password)
                .age(registerRequest.age())
                .name(registerRequest.name())
                .nickname(registerRequest.nickName())
                .friendCode(generateUniqueFriendCode())
                .status(UserStatus.ACTIVE)
                .gender(gender)
                .loginType(loginType)
                .createdAt(LocalDateTime.now())
                .uuid(userUuid)
                .build();
        userRepository.save(user);
        log.debug("service end");

        return userUuid;
    }


    @Override
    public void updateNickname(Long userId, String name) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
        user.updateNickname(name);
    }

    @Override
    public List<User> getUserIdByUserUuids(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }

        return userRepository.findAllByUuidIn(uuids);
    }

    // uuid로 user id 찾는 메서드
    @Override
    public Long getUserIdByUserUuid(String userUuid) {
        log.debug("User Service getUserIdByUserUuid start");

        return userRepository.getUserIdByUserUuid(userUuid)
            .orElseThrow(UserNotFoundException::new);

    }

    @Override
    public String getUuidByUserId(Long userId) {
        return userRepository.findUuidById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    @Override
    public boolean validateUniqueInputId(String inputId) {
        return !userRepository.existsByInputId(inputId);
    }

    private String generateUniqueFriendCode() {
        for (int i = 0; i < FRIEND_CODE_MAX_RETRY; i++) {
            String friendCode = generateFriendCode();

            if (!userRepository.existsByFriendCode(friendCode)) {
                return friendCode;
            }
        }

        throw new IllegalStateException("친구 코드를 생성하지 못했습니다.");
    }

    private String generateFriendCode() {
        StringBuilder builder = new StringBuilder(FRIEND_CODE_PREFIX);

        for (int i = 0; i < FRIEND_CODE_RANDOM_LENGTH; i++) {
            int index = secureRandom.nextInt(FRIEND_CODE_CHARS.length());
            builder.append(FRIEND_CODE_CHARS.charAt(index));
        }

        return builder.toString();
    }

}
