package com.chat_server.chatroom.service.impl;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.enums.MessageType;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.util.ChatRoomHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatRoomServiceImplTest {

    private ChatRoomRepository chatRoomRepository;
    private UserRepository userRepository;
    private ChatRoomServiceImpl service;

    @BeforeEach
    void setUp() {
        chatRoomRepository = mock(ChatRoomRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ChatRoomServiceImpl(chatRoomRepository, userRepository);
    }

    /** 기존 DM이 있으면 사용자 조회와 저장 없이 기존 방을 반환하는지 검증한다. */
    @Test
    @DisplayName("1대1 채팅방 조회 성공 - 기존 DM이 있으면 저장 없이 기존 roomId를 반환한다")
    void getOrCreateOneToOneChatRoomShouldReturnExistingRoom() {
        String dmKey = ChatRoomHashUtil.createUserPairHash(1L, 2L);
        when(chatRoomRepository.findRoomIdByDmKeyAndRoomType(dmKey, RoomType.DM))
                .thenReturn(Optional.of(100L));

        ChatRoomResult result = service.getOrCreateOneToOneChatRoom(1L, 2L);

        assertThat(result.roomId()).isEqualTo(100L);
        assertThat(result.created()).isFalse();
        verifyNoInteractions(userRepository);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    /** 기존 DM이 없으면 DM 정책값으로 새 방을 생성하는지 검증한다. */
    @Test
    @DisplayName("1대1 채팅방 생성 성공 - 기존 방이 없으면 DM 정책으로 새 방을 저장한다")
    void getOrCreateOneToOneChatRoomShouldCreateNewRoom() {
        User creator = user(1L, "user-1");
        String dmKey = ChatRoomHashUtil.createUserPairHash(1L, 2L);
        ChatRoom saved = ChatRoom.builder()
                .id(200L)
                .roomType(RoomType.DM)
                .dmKey(dmKey)
                .createdBy(creator)
                .maxPerson(2)
                .participantCount(2)
                .build();

        when(chatRoomRepository.findRoomIdByDmKeyAndRoomType(dmKey, RoomType.DM))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(saved);

        ChatRoomResult result = service.getOrCreateOneToOneChatRoom(1L, 2L);

        assertThat(result.roomId()).isEqualTo(200L);
        assertThat(result.created()).isTrue();

        ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository).save(captor.capture());
        ChatRoom created = captor.getValue();
        assertThat(created.getRoomType()).isEqualTo(RoomType.DM);
        assertThat(created.getDmKey()).isEqualTo(dmKey);
        assertThat(created.getCreatedBy()).isSameAs(creator);
        assertThat(created.getMaxPerson()).isEqualTo(2);
        assertThat(created.getParticipantCount()).isEqualTo(2);
    }

    /** 생성자 사용자가 없으면 저장을 중단하고 도메인 예외를 발생시키는지 검증한다. */
    @Test
    @DisplayName("1대1 채팅방 생성 실패 - 생성자 사용자가 없으면 UserNotFoundException을 발생시킨다")
    void getOrCreateOneToOneChatRoomShouldFailWhenCreatorDoesNotExist() {
        String dmKey = ChatRoomHashUtil.createUserPairHash(1L, 2L);
        when(chatRoomRepository.findRoomIdByDmKeyAndRoomType(dmKey, RoomType.DM))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrCreateOneToOneChatRoom(1L, 2L))
                .isInstanceOf(UserNotFoundException.class);

        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    /** 동시 생성 충돌 후 다른 트랜잭션이 만든 방을 재조회해 반환하는지 검증한다. */
    @Test
    @DisplayName("1대1 채팅방 동시성 복구 - unique key 충돌 후 기존 방을 재조회해 반환한다")
    void getOrCreateOneToOneChatRoomShouldRecoverFromConcurrentInsertConflict() {
        User creator = user(1L, "user-1");
        String dmKey = ChatRoomHashUtil.createUserPairHash(1L, 2L);
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("duplicate dm_key");

        when(chatRoomRepository.findRoomIdByDmKeyAndRoomType(dmKey, RoomType.DM))
                .thenReturn(Optional.empty(), Optional.of(300L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenThrow(conflict);

        ChatRoomResult result = service.getOrCreateOneToOneChatRoom(1L, 2L);

        assertThat(result.roomId()).isEqualTo(300L);
        assertThat(result.created()).isFalse();
        verify(chatRoomRepository, times(2))
                .findRoomIdByDmKeyAndRoomType(dmKey, RoomType.DM);
    }

    /** 충돌 이후에도 방을 찾지 못하면 원래 DB 예외를 전파하는 한계를 검증한다. */
    @Test
    @DisplayName("1대1 채팅방 동시성 복구 실패 - 재조회도 실패하면 원래 DB 예외를 전파한다")
    void getOrCreateOneToOneChatRoomShouldRethrowConflictWhenRoomStillMissing() {
        User creator = user(1L, "user-1");
        String dmKey = ChatRoomHashUtil.createUserPairHash(1L, 2L);
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("duplicate dm_key");

        when(chatRoomRepository.findRoomIdByDmKeyAndRoomType(dmKey, RoomType.DM))
                .thenReturn(Optional.empty(), Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenThrow(conflict);

        assertThatThrownBy(() -> service.getOrCreateOneToOneChatRoom(1L, 2L))
                .isSameAs(conflict);
    }

    /** 사용자 ID 순서가 바뀌어도 동일한 DM 해시를 사용하는지 검증한다. */
    @Test
    @DisplayName("1대1 채팅방 해시 경계값 - 사용자 순서가 바뀌어도 같은 DM 방을 조회한다")
    void getOrCreateOneToOneChatRoomShouldUseOrderIndependentHash() {
        String expectedKey = ChatRoomHashUtil.createUserPairHash(1L, 9L);
        when(chatRoomRepository.findRoomIdByDmKeyAndRoomType(expectedKey, RoomType.DM))
                .thenReturn(Optional.of(90L));

        ChatRoomResult first = service.getOrCreateOneToOneChatRoom(1L, 9L);
        ChatRoomResult reverse = service.getOrCreateOneToOneChatRoom(9L, 1L);

        assertThat(first.roomId()).isEqualTo(reverse.roomId()).isEqualTo(90L);
        verify(chatRoomRepository, times(2))
                .findRoomIdByDmKeyAndRoomType(expectedKey, RoomType.DM);
    }

    /** 제목과 생성자를 포함한 GROUP 방을 저장하는지 검증한다. */
    @Test
    @DisplayName("그룹 채팅방 생성 성공 - 제목과 생성자를 가진 GROUP 방을 저장한다")
    void createGroupChatRoomShouldSaveGroupRoom() {
        User creator = user(5L, "creator");
        ChatRoom saved = ChatRoom.builder()
                .id(500L)
                .roomType(RoomType.GROUP)
                .name("팀 채팅")
                .createdBy(creator)
                .build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(creator));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(saved);

        ChatRoom result = service.createGroupChatRoom("팀 채팅", 5L);

        assertThat(result.getId()).isEqualTo(500L);
        ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository).save(captor.capture());
        assertThat(captor.getValue().getRoomType()).isEqualTo(RoomType.GROUP);
        assertThat(captor.getValue().getName()).isEqualTo("팀 채팅");
        assertThat(captor.getValue().getCreatedBy()).isSameAs(creator);
    }

    /** 그룹 방 생성자가 없으면 저장을 수행하지 않는지 검증한다. */
    @Test
    @DisplayName("그룹 채팅방 생성 실패 - 생성자 사용자가 없으면 저장하지 않는다")
    void createGroupChatRoomShouldFailWhenCreatorIsMissing() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createGroupChatRoom("팀 채팅", 5L))
                .isInstanceOf(UserNotFoundException.class);

        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    /** 메시지 ID, 미리보기, 생성 시각이 방 메타데이터에 반영되는지 검증한다. */
    @Test
    @DisplayName("마지막 메시지 갱신 성공 - 메시지 메타데이터를 채팅방에 반영한다")
    void updateLastMessageInfoShouldUpdateRoomMetadata() {
        User sender = user(3L, "sender");
        ChatRoom room = ChatRoom.builder()
                .id(10L)
                .roomType(RoomType.GROUP)
                .createdBy(sender)
                .build();
        ChatMessage message = ChatMessage.create(room, sender, "안녕하세요", MessageType.TEXT.name(), "client-1");
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 20, 15, 0);
        ReflectionTestUtils.setField(message, "id", 777L);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);

        service.updateLastMessageInfo(room, message);

        assertThat(room.getLastMessageId()).isEqualTo(777L);
        assertThat(room.getLastMessagePreview()).isEqualTo("안녕하세요");
        assertThat(room.getLastMessageAt()).isEqualTo(createdAt);
    }

    /** 참여자 수 감소가 Repository update 메서드로 위임되는지 검증한다. */
    @Test
    @DisplayName("참여자 수 감소 성공 - 대상 roomId로 Repository 감소 쿼리를 호출한다")
    void decrementParticipantCountShouldDelegateToRepository() {
        service.decrementParticipantCount(55L);

        verify(chatRoomRepository).decrementParticipantCount(55L);
    }

    private User user(Long id, String nickname) {
        return User.builder()
                .id(id)
                .uuid("uuid-" + id)
                .nickname(nickname)
                .name(nickname)
                .inputId("input-" + id)
                .friendCode("friend-" + id)
                .build();
    }
}