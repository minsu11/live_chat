package com.chat_server.user.service.impl;

import com.chat_server.gender.entity.Gender;
import com.chat_server.gender.repository.GenderRepository;
import com.chat_server.logintype.entity.LoginType;
import com.chat_server.logintype.enums.LoginTypeEnum;
import com.chat_server.logintype.repository.LoginTypeRepository;
import com.chat_server.user.dto.request.UserRegisterRequest;
import com.chat_server.user.entity.User;
import com.chat_server.user.enums.UserStatus;
import com.chat_server.user.exception.UserAleadyExistException;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserServiceImplTest {
    @Test
    @DisplayName("회원가입 성공 시 비밀번호를 암호화하고 고유 친구코드와 UUID를 저장한다")
    void createUserShouldEncodePasswordAndSaveUserWithFriendCode() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        GenderRepository genderRepository = mock(GenderRepository.class);
        UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
        LoginTypeRepository loginTypeRepository = mock(LoginTypeRepository.class);
        UserServiceImpl service = new UserServiceImpl(userRepository, passwordEncoder, genderRepository, userProfileRepository, loginTypeRepository);
        Gender gender = mock(Gender.class);
        LoginType loginType = mock(LoginType.class);
        when(userRepository.existsByInputId("input01")).thenReturn(false);
        when(userRepository.existsByFriendCode(any())).thenReturn(false);
        when(genderRepository.findByName("MALE")).thenReturn(Optional.of(gender));
        when(loginTypeRepository.findByName(LoginTypeEnum.LOCAL.name())).thenReturn(Optional.of(loginType));
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        String uuid = service.createUSer(new UserRegisterRequest("input01", "plain-password", "홍길동", "길동", 20, "MALE", null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(uuid).isEqualTo(saved.getUuid());
        assertThat(saved.getInputPassword()).isEqualTo("encoded-password");
        assertThat(saved.getFriendCode()).startsWith("CTK-").hasSize(12);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("회원가입 실패 시 중복 아이디이면 저장 전에 예외를 발생시킨다")
    void createUserShouldThrowWhenInputIdAlreadyExists() {
        UserRepository userRepository = mock(UserRepository.class);
        UserServiceImpl service = new UserServiceImpl(userRepository, mock(PasswordEncoder.class), mock(GenderRepository.class), mock(UserProfileRepository.class), mock(LoginTypeRepository.class));
        when(userRepository.existsByInputId("input01")).thenReturn(true);

        assertThatThrownBy(() -> service.createUSer(new UserRegisterRequest("input01", "password", "홍길동", "길동", 20, "MALE", null, null)))
                .isInstanceOf(UserAleadyExistException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("닉네임 수정 성공 시 사용자 엔티티의 닉네임을 변경한다")
    void updateNicknameShouldUpdateUserNickname() {
        UserRepository userRepository = mock(UserRepository.class);
        UserServiceImpl service = new UserServiceImpl(userRepository, mock(PasswordEncoder.class), mock(GenderRepository.class), mock(UserProfileRepository.class), mock(LoginTypeRepository.class));
        User user = user(1L, "uuid", "old");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.updateNickname(1L, "new");

        assertThat(user.getNickname()).isEqualTo("new");
    }

    @Test
    @DisplayName("사용자 조회 실패 시 UserNotFoundException을 발생시킨다")
    void lookupMethodsShouldThrowWhenUserIsMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        UserServiceImpl service = new UserServiceImpl(userRepository, mock(PasswordEncoder.class), mock(GenderRepository.class), mock(UserProfileRepository.class), mock(LoginTypeRepository.class));
        when(userRepository.getUserIdByUserUuid("missing")).thenReturn(Optional.empty());
        when(userRepository.findUuidById(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserIdByUserUuid("missing")).isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> service.getUuidByUserId(1L)).isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> service.getUserById(1L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("여러 UUID 조회 성공 시 null 또는 빈 목록은 빈 리스트를 반환하고 값이 있으면 repository 결과를 반환한다")
    void getUserIdByUserUuidsShouldHandleEmptyAndDelegate() {
        UserRepository userRepository = mock(UserRepository.class);
        UserServiceImpl service = new UserServiceImpl(userRepository, mock(PasswordEncoder.class), mock(GenderRepository.class), mock(UserProfileRepository.class), mock(LoginTypeRepository.class));
        List<User> users = List.of(user(1L, "u1", "n1"));
        when(userRepository.findAllByUuidIn(List.of("u1"))).thenReturn(users);

        assertThat(service.getUserIdByUserUuids(null)).isEmpty();
        assertThat(service.getUserIdByUserUuids(List.of())).isEmpty();
        assertThat(service.getUserIdByUserUuids(List.of("u1"))).isEqualTo(users);
    }

    @Test
    @DisplayName("아이디 중복 검증 성공 시 existsByInputId의 반대 값을 반환한다")
    void validateUniqueInputIdShouldReturnOppositeOfExists() {
        UserRepository userRepository = mock(UserRepository.class);
        UserServiceImpl service = new UserServiceImpl(userRepository, mock(PasswordEncoder.class), mock(GenderRepository.class), mock(UserProfileRepository.class), mock(LoginTypeRepository.class));
        when(userRepository.existsByInputId("used")).thenReturn(true);
        when(userRepository.existsByInputId("new")).thenReturn(false);

        assertThat(service.validateUniqueInputId("used")).isFalse();
        assertThat(service.validateUniqueInputId("new")).isTrue();
    }

    private User user(Long id, String uuid, String nickname) {
        return User.builder().id(id).uuid(uuid).nickname(nickname).name(nickname)
                .inputId("input-" + id).friendCode("friend-" + id).status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build();
    }
}
