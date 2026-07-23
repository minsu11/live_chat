package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.exception.ChatMessageNotFoundException;
import com.chat_server.chatmessage.repository.ChatMessageRepository;
import com.chat_server.chatread.dto.event.UpdatedMessageUnreadCount;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatMessageServiceImplTest {

    private ChatMessageRepository chatMessageRepository;
    private UserRepository userRepository;
    private CustomProperties customProperties;
    private ChatMessageServiceImpl target;

    @BeforeEach
    void setUp() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        userRepository = mock(UserRepository.class);
        customProperties = mock(CustomProperties.class);
        target = new ChatMessageServiceImpl(chatMessageRepository, userRepository, customProperties);
    }

    @Test
    @DisplayName("메시지 생성 성공 시 사용자 조회 후 클라이언트 메시지 ID를 정리해서 저장한다")
    void createChatMessageShouldFindSenderNormalizeClientMessageIdAndSave() {
        ChatRoom room = chatRoom(10L);
        User sender = user(1L, "sender-uuid", "세인");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        ChatMessage result = target.createChatMessage(room, 1L, "text", "안녕하세요", "  client-1  ");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        ChatMessage saved = captor.getValue();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(saved.getChatRoom()).isSameAs(room);
        assertThat(saved.getSender()).isSameAs(sender);
        assertThat(saved.getMessageType().name()).isEqualTo("TEXT");
        assertThat(saved.getMessageContent()).isEqualTo("안녕하세요");
        assertThat(saved.getClientMessageId()).isEqualTo("client-1");
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("메시지 생성 실패 시 발신 사용자가 없으면 저장하지 않고 예외를 발생시킨다")
    void createChatMessageShouldThrowWhenSenderDoesNotExist() {
        ChatRoom room = chatRoom(10L);
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> target.createChatMessage(room, 404L, "text", "내용", "client-404"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("user not found");

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Redis 읽음 맵 기준 안읽음 수 계산 성공 시 최근 50개 메시지의 카운트를 반환한다")
    void calculateUnreadCountsWithRedisShouldReturnUnreadCountsForRecentMessages() {
        Long roomId = 30L;
        ChatMessage message10 = message(roomId, 10L, user(1L, "u1", "나"), "m10");
        ChatMessage message20 = message(roomId, 20L, user(2L, "u2", "친구"), "m20");
        when(chatMessageRepository.findRecentMessages(eq(roomId), eq(20L), any(Pageable.class)))
                .thenReturn(List.of(message10, message20));
        Map<Long, Long> memberReadMap = Map.of(
                1L, 20L,
                2L, 10L,
                3L, 0L
        );

        List<UpdatedMessageUnreadCount> result = target.calculateUnreadCountsWithRedis(roomId, 20L, memberReadMap, 3);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(chatMessageRepository).findRecentMessages(eq(roomId), eq(20L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 50));
        assertThat(result)
                .extracting(UpdatedMessageUnreadCount::messageId, UpdatedMessageUnreadCount::unreadCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, 1),
                        org.assertj.core.groups.Tuple.tuple(20L, 2)
                );
    }

    @Test
    @DisplayName("Redis 읽음 맵 계산 성공 시 읽은 사용자가 전체 인원보다 많아도 안읽음 수는 0 미만이 되지 않는다")
    void calculateUnreadCountsWithRedisShouldClampUnreadCountAtZero() {
        Long roomId = 31L;
        ChatMessage message = message(roomId, 5L, user(1L, "u1", "나"), "m5");
        when(chatMessageRepository.findRecentMessages(eq(roomId), eq(5L), any(Pageable.class)))
                .thenReturn(List.of(message));
        Map<Long, Long> memberReadMap = Map.of(1L, 5L, 2L, 5L, 3L, 5L);

        List<UpdatedMessageUnreadCount> result = target.calculateUnreadCountsWithRedis(roomId, 5L, memberReadMap, 2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).messageId()).isEqualTo(5L);
        assertThat(result.get(0).unreadCount()).isZero();
    }

    @Test
    @DisplayName("문맥 메시지 조회 성공 시 이전 메시지는 시간순으로 뒤집고 대상 메시지와 이후 메시지를 이어 붙인다")
    void getContextMessagesShouldCombineOlderTargetAndNewerMessagesInDisplayOrder() {
        Long roomId = 40L;
        Long targetMessageId = 100L;

        ChatMessage older90 = message(roomId, 90L, user(1L, "u1", "나"), "older90");
        ChatMessage older80 = message(roomId, 80L, user(1L, "u1", "나"), "older80");
        ChatMessage targetMessage = message(roomId, targetMessageId, user(2L, "u2", "친구"), "target");
        ChatMessage newer110 = message(roomId, 110L, user(3L, "u3", "다른친구"), "newer110");

        when(chatMessageRepository.findByIdAndChatRoom_Id(targetMessageId, roomId))
                .thenReturn(Optional.of(targetMessage));

        when(chatMessageRepository.findOlderMessagesWithTarget(roomId, targetMessageId, 3))
                .thenReturn(List.of(targetMessage, older90, older80));

        when(chatMessageRepository.findNewerMessages(roomId, targetMessageId, 3))
                .thenReturn(List.of(newer110));

        List<ChatMessage> result = target.getContextMessages(roomId, targetMessageId, 3);

        assertThat(result)
                .extracting(ChatMessage::getId)
                .containsExactly(80L, 90L, 100L, 110L);
    }

    @Test
    @DisplayName("문맥 메시지 조회 실패 시 대상 메시지가 없으면 설정 메시지를 담은 예외를 발생시킨다")
    void getContextMessagesShouldThrowWhenTargetMessageDoesNotExist() {
        Long roomId = 40L;
        Long targetMessageId = 999L;
        CustomProperties.Error error = mock(CustomProperties.Error.class);
        when(customProperties.getError()).thenReturn(error);
        when(error.getMessage(ErrorCode.CHAT_MESSAGE_NOT_FOUND)).thenReturn("메시지를 찾을 수 없습니다.");
        when(chatMessageRepository.findByIdAndChatRoom_Id(targetMessageId, roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> target.getContextMessages(roomId, targetMessageId, 10))
                .isInstanceOf(ChatMessageNotFoundException.class)
                .hasMessageContaining("메시지를 찾을 수 없습니다.");

        verify(chatMessageRepository, never()).findOlderMessagesWithTarget(any(), any(), anyInt());
        verify(chatMessageRepository, never()).findNewerMessages(any(), any(), anyInt());
    }

    private ChatRoom chatRoom(Long roomId) {
        return ChatRoom.builder()
                .id(roomId)
                .roomType(RoomType.GROUP)
                .name("room")
                .createdBy(user(99L, "creator", "방장"))
                .build();
    }

    private User user(Long id, String uuid, String nickname) {
        return User.builder()
                .id(id)
                .uuid(uuid)
                .nickname(nickname)
                .name(nickname)
                .inputId("input-" + id)
                .friendCode("friend-" + id)
                .build();
    }

    private ChatMessage message(Long roomId, Long messageId, User sender, String content) {
        ChatMessage chatMessage = ChatMessage.create(
                chatRoom(roomId),
                sender,
                content,
                "TEXT",
                "client-" + messageId
        );
        ReflectionTestUtils.setField(chatMessage, "id", messageId);
        ReflectionTestUtils.setField(chatMessage, "createdAt", LocalDateTime.of(2026, 6, 11, 12, 0).plusMinutes(messageId));
        return chatMessage;
    }
}
