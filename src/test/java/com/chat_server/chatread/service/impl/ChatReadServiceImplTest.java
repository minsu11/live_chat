package com.chat_server.chatread.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatReadServiceImplTest {
    @Test
    @DisplayName("실시간 읽음 처리 성공 시 ChatList의 lastReadMessageId 갱신을 위임한다")
    void markAsReadShouldDelegateToChatListMarkAsRead() {
        ChatListService chatListService = mock(ChatListService.class);
        ChatReadServiceImpl service = new ChatReadServiceImpl(chatListService);
        when(chatListService.markAsRead(10L, 1L, 100L)).thenReturn(1);

        service.markAsRead(10L, 1L, 100L);

        verify(chatListService).markAsRead(10L, 1L, 100L);
    }

    @Test
    @DisplayName("실시간 읽음 처리 실패 시 채팅 목록 row가 없으면 예외를 발생시킨다")
    void markAsReadShouldThrowWhenMembershipIsMissing() {
        ChatListService chatListService = mock(ChatListService.class);
        ChatReadServiceImpl service = new ChatReadServiceImpl(chatListService);
        when(chatListService.markAsRead(10L, 1L, 100L)).thenReturn(0);

        assertThatThrownBy(() -> service.markAsRead(10L, 1L, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chat_list 멤버십이 없어");
    }

    @Test
    @DisplayName("채팅방 진입 읽음 처리 성공 시 null 최신 메시지는 0으로 보정해 위임한다")
    void markAsReadOnEnterShouldUseZeroWhenLatestMessageIdIsNull() {
        ChatListService chatListService = mock(ChatListService.class);
        ChatReadServiceImpl service = new ChatReadServiceImpl(chatListService);
        when(chatListService.markAsRead(10L, 1L, 0L)).thenReturn(1);

        service.markAsReadOnEnter(10L, 1L, null);

        verify(chatListService).markAsRead(10L, 1L, 0L);
    }

    @Test
    @DisplayName("채팅방 진입 읽음 처리 실패 시 멤버십이 없으면 예외를 발생시킨다")
    void markAsReadOnEnterShouldThrowWhenMembershipIsMissing() {
        ChatListService chatListService = mock(ChatListService.class);
        ChatReadServiceImpl service = new ChatReadServiceImpl(chatListService);
        when(chatListService.markAsRead(10L, 1L, 100L)).thenReturn(0);

        assertThatThrownBy(() -> service.markAsReadOnEnter(10L, 1L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("멤버쉽 없음");
    }
}
