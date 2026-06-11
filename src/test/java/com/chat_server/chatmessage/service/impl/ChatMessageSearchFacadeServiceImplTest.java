package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.dto.response.ChatMessageSearchPageResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.common.cursor.ChatMessageCursorCodec;
import com.chat_server.user.entity.User;
import com.chat_server.user.service.UserDisplayNameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatMessageSearchFacadeServiceImplTest {
    private ChatMessageSearchServiceImpl searchService;
    private ChatRoomQueryService roomQueryService;
    private UserDisplayNameService displayNameService;
    private ChatMessageSearchFacadeServiceImpl facade;

    @BeforeEach
    void setUp() {
        searchService = mock(ChatMessageSearchServiceImpl.class);
        roomQueryService = mock(ChatRoomQueryService.class);
        displayNameService = mock(UserDisplayNameService.class);
        facade = new ChatMessageSearchFacadeServiceImpl(searchService, roomQueryService, displayNameService);
    }

    @Test
    @DisplayName("메시지 검색 성공 시 멤버 검증 후 limit+1 조회로 hasNext와 nextCursor를 만든다")
    void searchChatMessagesShouldValidateMemberAndReturnPageWithNextCursor() {
        ChatMessage first = message(100L, 2L, "sender-2", "기본이름", "hello", LocalDateTime.of(2026, 6, 11, 12, 0));
        ChatMessage second = message(90L, 3L, "sender-3", "다음", "hello2", LocalDateTime.of(2026, 6, 11, 11, 0));
        when(searchService.searchMessages(eq(10L), eq("hello"), isNull(), isNull(), eq(2)))
                .thenReturn(List.of(first, second));
        when(displayNameService.resolveDisplayNamesBulk(1L, List.of(2L))).thenReturn(Map.of(2L, "친구별칭"));

        ChatMessageSearchPageResponse response = facade.searchChatMessages(10L, 1L, "hello", null, 1);

        verify(roomQueryService).validateMemberOrThrow(10L, 1L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).senderNickname()).isEqualTo("친구별칭");
        assertThat(response.hasNext()).isTrue();
        assertThat(ChatMessageCursorCodec.decode(response.nextCursor()).lastMessageId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("메시지 검색 실패 시 멤버가 아니면 검색 서비스를 호출하지 않는다")
    void searchChatMessagesShouldStopWhenMembershipValidationFails() {
        doThrow(new IllegalArgumentException("not member")).when(roomQueryService).validateMemberOrThrow(10L, 1L);

        assertThatThrownBy(() -> facade.searchChatMessages(10L, 1L, "hello", null, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("not member");

        verify(searchService, never()).searchMessages(any(), any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("검색어가 비어 있으면 멤버 검증 후 빈 페이지를 반환한다")
    void searchChatMessagesShouldReturnEmptyPageWhenKeywordIsBlank() {
        ChatMessageSearchPageResponse response = facade.searchChatMessages(10L, 1L, " ", null, 50);

        assertThat(response.items()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.hasNext()).isFalse();
        verify(searchService, never()).searchMessages(any(), any(), any(), any(), anyInt());
    }

    private ChatMessage message(Long id, Long senderId, String senderUuid, String senderNickname, String content, LocalDateTime createdAt) {
        User sender = User.builder().id(senderId).uuid(senderUuid).nickname(senderNickname).name(senderNickname)
                .inputId("input-" + senderId).friendCode("code-" + senderId).build();
        ChatRoom room = ChatRoom.builder().id(10L).roomType(RoomType.GROUP).name("room").createdBy(sender).build();
        ChatMessage message = ChatMessage.create(room, sender, content, "TEXT", "client-" + id);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        return message;
    }
}
