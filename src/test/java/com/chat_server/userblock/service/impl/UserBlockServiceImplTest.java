package com.chat_server.userblock.service.impl;

import com.chat_server.userblock.exception.UserBlockExistsException;
import com.chat_server.userblock.repository.UserBlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserBlockServiceImplTest {
    @Test
    @DisplayName("차단 검증 성공 시 양방향 차단 관계가 없으면 예외가 발생하지 않는다")
    void validateSenderNotBlockedShouldPassWhenNoBlockExists() {
        UserBlockRepository repository = mock(UserBlockRepository.class);
        UserBlockServiceImpl service = new UserBlockServiceImpl(repository);
        when(repository.existsByUserBlock(1L, 2L)).thenReturn(false);
        when(repository.existsByUserBlock(2L, 1L)).thenReturn(false);

        service.validateSenderNotBlocked(1L, 2L);

        assertThat(service.isBlocked(1L, 2L)).isFalse();
    }

    @Test
    @DisplayName("차단 검증 실패 시 발신자가 수신자를 차단했으면 예외가 발생한다")
    void validateSenderNotBlockedShouldThrowWhenSenderBlockedReceiver() {
        UserBlockRepository repository = mock(UserBlockRepository.class);
        UserBlockServiceImpl service = new UserBlockServiceImpl(repository);
        when(repository.existsByUserBlock(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.validateSenderNotBlocked(1L, 2L))
                .isInstanceOf(UserBlockExistsException.class);
        assertThat(service.isBlocked(1L, 2L)).isTrue();
    }

    @Test
    @DisplayName("차단 검증 실패 시 수신자가 발신자를 차단했으면 예외가 발생한다")
    void validateSenderNotBlockedShouldThrowWhenReceiverBlockedSender() {
        UserBlockRepository repository = mock(UserBlockRepository.class);
        UserBlockServiceImpl service = new UserBlockServiceImpl(repository);
        when(repository.existsByUserBlock(1L, 2L)).thenReturn(false);
        when(repository.existsByUserBlock(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.validateSenderNotBlocked(1L, 2L))
                .isInstanceOf(UserBlockExistsException.class);
        assertThat(service.isBlocked(1L, 2L)).isTrue();
    }
}
