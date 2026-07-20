package com.chat_server.chatlist.service.impl;

import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.exception.ChatRoomNotFoundException;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroom.resolver.ChatRoomDisplayResolver;
import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.redis.dto.ChatRoomMetaDto;
import com.chat_server.redis.service.ChatMetadataRedisService;
import com.chat_server.user.entity.User;
import com.chat_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatListServiceImplAdditionalTest {

    private ChatListRepository chatListRepository;
    private ChatRoomDisplayResolver displayResolver;
    private ChatRoomRepository chatRoomRepository;
    private UserRepository userRepository;
    private ChatMetadataRedisService redisService;
    private ChatListServiceImpl service;

    @BeforeEach
    void setUp() {
        chatListRepository = mock(ChatListRepository.class);
        displayResolver = mock(ChatRoomDisplayResolver.class);
        chatRoomRepository = mock(ChatRoomRepository.class);
        userRepository = mock(UserRepository.class);
        redisService = mock(ChatMetadataRedisService.class);

        service = new ChatListServiceImpl(
                chatListRepository,
                displayResolver,
                mock(CustomProperties.class),
                chatRoomRepository,
                userRepository,
                redisService
        );
    }

    /**
     * 다음 페이지가 존재할 때 정렬 완료된 마지막 항목의 orderAt과 roomId로 커서를 생성하는지 검증한다.
     */
    @Test
    @DisplayName("채팅 목록 커서 생성 성공 - hasNext가 true이면 마지막 항목 기준 nextCursor를 반환한다")
    void getChatRoomListsByCursorShouldCreateNextCursorWhenNextPageExists() {
        LocalDateTime firstTime = LocalDateTime.of(2026, 7, 20, 12, 0);
        LocalDateTime lastTime = LocalDateTime.of(2026, 7, 20, 11, 0);
        ChatRoomListResponse first = roomList(1L, firstTime);
        ChatRoomListResponse last = roomList(2L, lastTime);

        when(chatListRepository.getChatRoomListByCursor(eq(10L), eq(2), isNull()))
                .thenReturn(new SliceImpl<>(List.of(first, last), PageRequest.of(0, 2), true));
        when(redisService.getUnreadCount(anyLong(), eq(10L))).thenReturn(0);
        when(redisService.getRoomMeta(anyLong())).thenReturn(null);
        when(displayResolver.resolveTitle(anyLong(), eq(10L))).thenReturn("채팅방");

        var result = service.getChatRoomListsByCursor(10L, 2, null);

        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isNotBlank();
        assertThat(result.items()).extracting(ChatRoomListResponse::roomId)
                .containsExactly(1L, 2L);
    }

    /**
     * 다음 페이지가 있는데 마지막 데이터의 정렬 기준 시간이 null이면 안정적인 커서를 만들 수 없으므로
     * 명시적인 예외를 발생시키는지 검증한다.
     */
    @Test
    @DisplayName("채팅 목록 커서 생성 실패 - hasNext인데 마지막 orderAt이 null이면 예외를 발생시킨다")
    void getChatRoomListsByCursorShouldThrowWhenLastOrderAtIsNull() {
        ChatRoomListResponse item = new ChatRoomListResponse(
                1L, "room", 0, "preview", null, null, false);
        when(chatListRepository.getChatRoomListByCursor(eq(10L), eq(1), isNull()))
                .thenReturn(new SliceImpl<>(List.of(item), PageRequest.of(0, 1), true));
        when(redisService.getUnreadCount(1L, 10L)).thenReturn(0);
        when(redisService.getRoomMeta(1L)).thenReturn(null);
        when(displayResolver.resolveTitle(1L, 10L)).thenReturn("채팅방");

        assertThatThrownBy(() -> service.getChatRoomListsByCursor(10L, 1, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("orderAt is null")
                .hasMessageContaining("roomId=1");
    }

    /**
     * Redis의 마지막 메시지 시간이 DB orderAt보다 오래된 경우 기존 정렬 시간을 유지하는지 검증한다.
     * 오래된 캐시가 채팅방 순서를 과거로 되돌리면 안 된다.
     */
    @Test
    @DisplayName("채팅 목록 메타 병합 경계값 - Redis 시간이 더 오래되면 DB orderAt을 유지한다")
    void getChatListItemShouldKeepDatabaseOrderWhenRedisTimeIsOlder() {
        LocalDateTime dbTime = LocalDateTime.of(2026, 7, 20, 13, 0);
        LocalDateTime oldRedisTime = LocalDateTime.of(2026, 7, 20, 12, 0);
        ChatListItemResponse item = new ChatListItemResponse(
                10L, "db", 2, "db-preview", dbTime, dbTime);

        when(chatListRepository.findChatListItem(10L, 1L)).thenReturn(Optional.of(item));
        when(redisService.getUnreadCount(10L, 1L)).thenReturn(3);
        when(redisService.getRoomMeta(10L))
                .thenReturn(new ChatRoomMetaDto(100L, "redis-preview", oldRedisTime));
        when(displayResolver.resolveTitle(10L, 1L)).thenReturn("표시 이름");

        ChatListItemResponse result = service.getChatListItem(10L, 1L);

        assertThat(result.orderAt()).isEqualTo(dbTime);
        assertThat(result.lastMessageAt()).isEqualTo(oldRedisTime);
        assertThat(result.lastMessagePreview()).isEqualTo("redis-preview");
        assertThat(result.unreadCount()).isEqualTo(3);
    }

    /**
     * 채팅 목록 row가 없을 때 Redis 조회를 진행하지 않고 명확한 예외를 발생시키는지 검증한다.
     */
    @Test
    @DisplayName("채팅 목록 단건 조회 실패 - DB row가 없으면 예외를 발생시키고 Redis를 조회하지 않는다")
    void getChatListItemShouldThrowWhenChatListRowIsMissing() {
        when(chatListRepository.findChatListItem(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getChatListItem(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roomId=10")
                .hasMessageContaining("userId=1");

        verifyNoInteractions(redisService, displayResolver);
    }

    /**
     * unread 조회 대상이 null이면 Repository IN 쿼리를 실행하지 않고 빈 Map을 반환하는지 검증한다.
     */
    @Test
    @DisplayName("안읽음 수 일괄 조회 경계값 - 사용자 목록이 null이면 빈 Map을 반환한다")
    void getUnreadCountMapShouldReturnEmptyMapWhenUserIdsAreNull() {
        Map<Long, Integer> result = service.getUnreadCountMap(10L, null);

        assertThat(result).isEmpty();
        verify(chatListRepository, never()).findUnreadCountRows(anyLong(), anyList());
    }

    /**
     * 채팅 목록 bulk 조회 대상이 비어 있으면 Repository를 호출하지 않는지 검증한다.
     */
    @Test
    @DisplayName("채팅 목록 Bulk 조회 경계값 - 빈 사용자 목록이면 빈 Map을 반환한다")
    void getChatListItemsBulkShouldReturnEmptyMapWhenUserIdsAreEmpty() {
        Map<Long, ?> result = service.getChatListItemsBulk(1L, 10L, List.of());

        assertThat(result).isEmpty();
        verify(chatListRepository, never()).findChatListItemsBulk(anyLong(), anyList());
    }

    /**
     * 이미 멤버십이 존재하는 사용자를 제외하고 신규 사용자만 ChatList로 생성해 저장하는지 검증한다.
     */
    @Test
    @DisplayName("채팅 목록 멤버십 Bulk 생성 성공 - 기존 사용자를 제외한 신규 사용자만 저장한다")
    void ensureMembershipsBulkShouldSaveOnlyMissingUsers() {
        User user2 = user(2L);
        User user3 = user(3L);
        ChatRoom room = room(10L);

        when(chatListRepository.findUserIdsByRoomIdAndUserIdIn(10L, List.of(1L, 2L, 3L)))
                .thenReturn(List.of(1L));
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(userRepository.getReferenceById(2L)).thenReturn(user2);
        when(userRepository.getReferenceById(3L)).thenReturn(user3);

        service.ensureMembershipsBulk(10L, List.of(1L, 2L, 3L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatList>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatListRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(chatList -> chatList.getUser().getId())
                .containsExactly(2L, 3L);
        assertThat(captor.getValue()).allSatisfy(chatList -> {
            assertThat(chatList.getChatRoom()).isSameAs(room);
            assertThat(chatList.getUnreadCount()).isZero();
        });
    }

    /**
     * 전달된 사용자 모두 멤버십이 존재하면 빈 saveAll을 호출하지 않는지 검증한다.
     */
    @Test
    @DisplayName("채팅 목록 멤버십 Bulk 생성 경계값 - 모든 사용자가 기존 멤버면 저장을 생략한다")
    void ensureMembershipsBulkShouldSkipSaveWhenAllUsersExist() {
        ChatRoom room = room(10L);
        List<Long> userIds = List.of(1L, 2L);
        when(chatListRepository.findUserIdsByRoomIdAndUserIdIn(10L, userIds))
                .thenReturn(userIds);
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room));

        service.ensureMembershipsBulk(10L, userIds);

        verify(chatListRepository, never()).saveAll(anyList());
        verifyNoInteractions(userRepository);
    }

    /**
     * 대상 채팅방이 존재하지 않으면 사용자 proxy 조회와 저장을 중단하는지 검증한다.
     */
    @Test
    @DisplayName("채팅 목록 멤버십 Bulk 생성 실패 - 채팅방이 없으면 저장하지 않고 예외를 발생시킨다")
    void ensureMembershipsBulkShouldThrowWhenRoomIsMissing() {
        when(chatListRepository.findUserIdsByRoomIdAndUserIdIn(10L, List.of(1L, 2L)))
                .thenReturn(List.of());
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ensureMembershipsBulk(10L, List.of(1L, 2L)))
                .isInstanceOf(ChatRoomNotFoundException.class);

        verifyNoInteractions(userRepository);
        verify(chatListRepository, never()).saveAll(anyList());
    }

    /**
     * 단건 멤버십, 안읽음 증가, 방 나가기 메서드가 정확한 인자로 Repository에 위임되는지 검증한다.
     */
    @Test
    @DisplayName("채팅 목록 단순 명령 성공 - 멤버십 생성, unread 증가, 방 나가기를 Repository에 위임한다")
    void simpleCommandsShouldDelegateToRepository() {
        service.ensureMembership(10L, 1L);
        service.increaseUnreadCount(10L, 1L);
        service.leaveChatRoom(10L, 1L);

        verify(chatListRepository).upsertMembership(10L, 1L);
        verify(chatListRepository).increaseUnreadCount(10L, 1L);
        verify(chatListRepository).deleteByChatRoomIdAndUserIdDirectly(10L, 1L);
    }

    private ChatRoomListResponse roomList(Long roomId, LocalDateTime orderAt) {
        return new ChatRoomListResponse(
                roomId, "room-" + roomId, 0, "preview", orderAt, orderAt, false);
    }

    private ChatRoom room(Long id) {
        return ChatRoom.builder()
                .id(id)
                .roomType(RoomType.GROUP)
                .createdBy(user(99L))
                .build();
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .uuid("uuid-" + id)
                .nickname("user-" + id)
                .name("user-" + id)
                .inputId("input-" + id)
                .friendCode("friend-" + id)
                .build();
    }
}