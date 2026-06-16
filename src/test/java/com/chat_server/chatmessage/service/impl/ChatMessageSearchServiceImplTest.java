package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.repository.ChatMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatMessageSearchServiceImplTest {
    @Test
    @DisplayName("검색 성공 시 검색어를 Boolean Mode 키워드로 변환하고 채팅방 단위 repository 검색을 호출한다")
    void searchMessagesShouldSanitizeKeywordAndDelegateRoomScopedSearch() {
        ChatMessageRepository repository = mock(ChatMessageRepository.class);
        ChatMessageSearchServiceImpl service = new ChatMessageSearchServiceImpl(repository);
        LocalDateTime cursorAt = LocalDateTime.of(2026, 6, 11, 12, 0);
        when(repository.searchMessagesByKeyword(10L, "hello -world*", "+hello* +world*", cursorAt, 100L, 20))
                .thenReturn(List.of(mock(ChatMessage.class)));

        List<ChatMessage> result = service.searchMessages(10L, " hello -world* ", cursorAt, 100L, 20);

        assertThat(result).hasSize(1);
        verify(repository).searchMessagesByKeyword(10L, "hello -world*", "+hello* +world*", cursorAt, 100L, 20);
    }

    @Test
    @DisplayName("검색어가 비어 있으면 repository를 호출하지 않고 빈 결과를 반환한다")
    void searchMessagesShouldReturnEmptyWhenKeywordIsBlank() {
        ChatMessageRepository repository = mock(ChatMessageRepository.class);
        ChatMessageSearchServiceImpl service = new ChatMessageSearchServiceImpl(repository);

        assertThat(service.searchMessages(10L, " ", null, null, 20)).isEmpty();
        assertThat(service.searchMessages(10L, null, null, null, 20)).isEmpty();
        verify(repository, never()).searchMessagesByKeyword(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("검색 limit는 1 이상 101 이하로 보정해서 repository에 전달한다")
    void searchMessagesShouldClampLimit() {
        ChatMessageRepository repository = mock(ChatMessageRepository.class);
        ChatMessageSearchServiceImpl service = new ChatMessageSearchServiceImpl(repository);

        service.searchMessages(10L, "hello", null, null, 0);
        service.searchMessages(10L, "hello", null, null, 999);

        verify(repository).searchMessagesByKeyword(10L, "hello", "+hello*", null, null, 1);
        verify(repository).searchMessagesByKeyword(10L, "hello", "+hello*", null, null, 101);
    }
}
