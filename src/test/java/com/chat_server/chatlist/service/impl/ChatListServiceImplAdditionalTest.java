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
import com.chat_server.common.cursor.ChatListCursorCodec;
import com.chat_server.common.cursor.ChatListCursorKey;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
     * 다음 페이지가 존재할 때 정렬이 완료된 마지막 항목의
     * orderAt과 roomId를 기준으로 다음 커서를 생성하는지 검증한다.
     *
     * 단순히 커서 문자열이 존재하는지만 확인하지 않고,
     * 생성된 커서를 다시 디코딩하여 시간과 채팅방 ID가 정확한지 검증한다.
     */
    @Test
    @DisplayName("채팅 목록 커서 생성 성공 - hasNext가 true이면 마지막 항목 기준 next 커서를 반환한다")
    void getChatRoomListsByCursorShouldCreateNextCursorWhenNextPageExists() {
        LocalDateTime firstTime =
                LocalDateTime.of(2026, 7, 20, 12, 0);

        LocalDateTime lastTime =
                LocalDateTime.of(2026, 7, 20, 11, 0);

        ChatRoomListResponse first = roomList(1L, firstTime);
        ChatRoomListResponse last = roomList(2L, lastTime);

        when(chatListRepository.getChatRoomListByCursor(
                eq(10L),
                eq(2),
                isNull()
        )).thenReturn(
                new SliceImpl<>(
                        List.of(first, last),
                        PageRequest.of(0, 2),
                        true
                )
        );

        when(redisService.getUnreadCount(anyLong(), eq(10L)))
                .thenReturn(0);

        when(redisService.getRoomMeta(anyLong()))
                .thenReturn(null);

        when(displayResolver.resolveTitle(anyLong(), eq(10L)))
                .thenReturn("채팅방");

        var result =
                service.getChatRoomListsByCursor(10L, 2, null);

        assertThat(result.hasNext()).isTrue();

        // CursorPageResponse의 필드명은 next이므로 next()를 사용한다.
        assertThat(result.next()).isNotBlank();

        assertThat(result.items())
                .extracting(ChatRoomListResponse::roomId)
                .containsExactly(1L, 2L);

        long expectedEpochMillis = lastTime
                .atOffset(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();

        ChatListCursorKey decodedCursor =
                ChatListCursorCodec.decode(result.next());

        assertThat(decodedCursor)
                .isNotNull()
                .satisfies(cursor -> {
                    assertThat(cursor.lastAtEpochMillis())
                            .isEqualTo(expectedEpochMillis);

                    assertThat(cursor.lastRoomId())
                            .isEqualTo(2L);
                });
    }

    /**
     * 다음 페이지가 존재하지만 마지막 데이터의 orderAt이 null인 상황을 검증한다.
     *
     * 정렬 기준 시간이 없으면 안정적인 다음 커서를 만들 수 없으므로
     * IllegalStateException이 발생해야 한다.
     */
    @Test
    @DisplayName("채팅 목록 커서 생성 실패 - hasNext인데 마지막 orderAt이 null이면 예외를 발생시킨다")
    void getChatRoomListsByCursorShouldThrowWhenLastOrderAtIsNull() {
        ChatRoomListResponse item = new ChatRoomListResponse(
                1L,
                "room",
                0,
                "preview",
                null,
                null,
                false
        );

        when(chatListRepository.getChatRoomListByCursor(
                eq(10L),
                eq(1),
                isNull()
        )).thenReturn(
                new SliceImpl<>(
                        List.of(item),
                        PageRequest.of(0, 1),
                        true
                )
        );

        when(redisService.getUnreadCount(1L, 10L))
                .thenReturn(0);

        when(redisService.getRoomMeta(1L))
                .thenReturn(null);

        when(displayResolver.resolveTitle(1L, 10L))
                .thenReturn("채팅방");

        assertThatThrownBy(
                () -> service.getChatRoomListsByCursor(10L, 1, null)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("orderAt is null")
                .hasMessageContaining("roomId=1");
    }

    /**
     * Redis에 저장된 마지막 메시지 시간이 DB의 orderAt보다 과거인 경우를 검증한다.
     *
     * 오래된 Redis 값으로 인해 채팅방 목록의 정렬 순서가
     * 과거 상태로 되돌아가면 안 되므로 DB orderAt을 유지해야 한다.
     */
    @Test
    @DisplayName("채팅 목록 메타 병합 경계값 - Redis 시간이 더 오래되면 DB orderAt을 유지한다")
    void getChatListItemShouldKeepDatabaseOrderWhenRedisTimeIsOlder() {
        LocalDateTime dbTime =
                LocalDateTime.of(2026, 7, 20, 13, 0);

        LocalDateTime oldRedisTime =
                LocalDateTime.of(2026, 7, 20, 12, 0);

        ChatListItemResponse item = new ChatListItemResponse(
                10L,
                "db",
                2,
                "db-preview",
                dbTime,
                dbTime
        );

        when(chatListRepository.findChatListItem(10L, 1L))
                .thenReturn(Optional.of(item));

        when(redisService.getUnreadCount(10L, 1L))
                .thenReturn(3);

        when(redisService.getRoomMeta(10L))
                .thenReturn(
                        new ChatRoomMetaDto(
                                100L,
                                "redis-preview",
                                oldRedisTime
                        )
                );

        when(displayResolver.resolveTitle(10L, 1L))
                .thenReturn("표시 이름");

        ChatListItemResponse result =
                service.getChatListItem(10L, 1L);

        assertThat(result.orderAt())
                .isEqualTo(dbTime);

        assertThat(result.lastMessageAt())
                .isEqualTo(oldRedisTime);

        assertThat(result.lastMessagePreview())
                .isEqualTo("redis-preview");

        assertThat(result.unreadCount())
                .isEqualTo(3);
    }

    /**
     * 채팅 목록 DB row가 존재하지 않는 경우를 검증한다.
     *
     * DB 조회 단계에서 실패하면 Redis 조회와 표시 이름 계산 같은
     * 후속 작업을 실행하지 않아야 한다.
     */
    @Test
    @DisplayName("채팅 목록 단건 조회 실패 - DB row가 없으면 예외를 발생시키고 Redis를 조회하지 않는다")
    void getChatListItemShouldThrowWhenChatListRowIsMissing() {
        when(chatListRepository.findChatListItem(10L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.getChatListItem(10L, 1L)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roomId=10")
                .hasMessageContaining("userId=1");

        verifyNoInteractions(redisService, displayResolver);
    }

    /**
     * unread count 조회 대상 사용자 목록이 null인 경계값을 검증한다.
     *
     * null 목록은 빈 결과로 처리하고 Repository의 IN 쿼리를
     * 실행하지 않아야 한다.
     */
    @Test
    @DisplayName("안읽음 수 일괄 조회 경계값 - 사용자 목록이 null이면 빈 Map을 반환한다")
    void getUnreadCountMapShouldReturnEmptyMapWhenUserIdsAreNull() {
        Map<Long, Integer> result =
                service.getUnreadCountMap(10L, null);

        assertThat(result).isEmpty();

        verify(chatListRepository, never())
                .findUnreadCountRows(anyLong(), anyList());
    }

    /**
     * 채팅 목록 Bulk 조회 대상 사용자 목록이 비어 있는 경우를 검증한다.
     *
     * 조회할 사용자가 없으면 Repository를 호출하지 않고
     * 즉시 빈 Map을 반환해야 한다.
     */
    @Test
    @DisplayName("채팅 목록 Bulk 조회 경계값 - 빈 사용자 목록이면 빈 Map을 반환한다")
    void getChatListItemsBulkShouldReturnEmptyMapWhenUserIdsAreEmpty() {
        Map<Long, ?> result =
                service.getChatListItemsBulk(
                        1L,
                        10L,
                        List.of()
                );

        assertThat(result).isEmpty();

        verify(chatListRepository, never())
                .findChatListItemsBulk(anyLong(), anyList());
    }

    /**
     * 요청된 사용자 중 이미 채팅방 멤버인 사용자를 제외하고
     * 신규 사용자만 ChatList 엔티티로 생성하는지 검증한다.
     *
     * 생성되는 ChatList의 초기 unreadCount는 0이어야 한다.
     */
    @Test
    @DisplayName("채팅 목록 멤버십 Bulk 생성 성공 - 기존 사용자를 제외한 신규 사용자만 저장한다")
    void ensureMembershipsBulkShouldSaveOnlyMissingUsers() {
        User user2 = user(2L);
        User user3 = user(3L);
        ChatRoom room = room(10L);

        List<Long> requestedUserIds =
                List.of(1L, 2L, 3L);

        when(chatListRepository.findUserIdsByRoomIdAndUserIdIn(
                10L,
                requestedUserIds
        )).thenReturn(List.of(1L));

        when(chatRoomRepository.findById(10L))
                .thenReturn(Optional.of(room));

        when(userRepository.getReferenceById(2L))
                .thenReturn(user2);

        when(userRepository.getReferenceById(3L))
                .thenReturn(user3);

        service.ensureMembershipsBulk(
                10L,
                requestedUserIds
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatList>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(chatListRepository)
                .saveAll(captor.capture());

        List<ChatList> savedChatLists =
                captor.getValue();

        assertThat(savedChatLists)
                .hasSize(2);

        assertThat(savedChatLists)
                .extracting(chatList -> chatList.getUser().getId())
                .containsExactly(2L, 3L);

        assertThat(savedChatLists)
                .allSatisfy(chatList -> {
                    assertThat(chatList.getChatRoom())
                            .isSameAs(room);

                    assertThat(chatList.getUnreadCount())
                            .isZero();
                });
    }

    /**
     * 전달된 사용자 모두 이미 채팅방 멤버인 경우를 검증한다.
     *
     * 새로 생성할 ChatList가 없으므로 사용자 Proxy 조회와
     * saveAll 호출을 생략해야 한다.
     */
    @Test
    @DisplayName("채팅 목록 멤버십 Bulk 생성 경계값 - 모든 사용자가 기존 멤버면 저장을 생략한다")
    void ensureMembershipsBulkShouldSkipSaveWhenAllUsersExist() {
        ChatRoom room = room(10L);
        List<Long> userIds = List.of(1L, 2L);

        when(chatListRepository.findUserIdsByRoomIdAndUserIdIn(
                10L,
                userIds
        )).thenReturn(userIds);

        when(chatRoomRepository.findById(10L))
                .thenReturn(Optional.of(room));

        service.ensureMembershipsBulk(
                10L,
                userIds
        );

        verify(chatListRepository, never())
                .saveAll(anyList());

        verifyNoInteractions(userRepository);
    }

    /**
     * Bulk 멤버십을 생성할 대상 채팅방이 존재하지 않는 경우를 검증한다.
     *
     * 채팅방 조회 실패 이후 사용자 Proxy 조회 및 ChatList 저장을
     * 진행하지 않아야 한다.
     */
    @Test
    @DisplayName("채팅 목록 멤버십 Bulk 생성 실패 - 채팅방이 없으면 저장하지 않고 예외를 발생시킨다")
    void ensureMembershipsBulkShouldThrowWhenRoomIsMissing() {
        List<Long> userIds = List.of(1L, 2L);

        when(chatListRepository.findUserIdsByRoomIdAndUserIdIn(
                10L,
                userIds
        )).thenReturn(List.of());

        when(chatRoomRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.ensureMembershipsBulk(
                        10L,
                        userIds
                )
        )
                .isInstanceOf(ChatRoomNotFoundException.class);

        verifyNoInteractions(userRepository);

        verify(chatListRepository, never())
                .saveAll(anyList());
    }

    /**
     * 단순 명령 메서드들이 정확한 roomId와 userId를 사용해
     * Repository 메서드로 위임되는지 검증한다.
     */
    @Test
    @DisplayName("채팅 목록 단순 명령 성공 - 멤버십 생성, unread 증가, 방 나가기를 Repository에 위임한다")
    void simpleCommandsShouldDelegateToRepository() {
        service.ensureMembership(10L, 1L);
        service.increaseUnreadCount(10L, 1L);
        service.leaveChatRoom(10L, 1L);

        verify(chatListRepository)
                .upsertMembership(10L, 1L);

        verify(chatListRepository)
                .increaseUnreadCount(10L, 1L);

        verify(chatListRepository)
                .deleteByChatRoomIdAndUserIdDirectly(10L, 1L);
    }

    private ChatRoomListResponse roomList(
            Long roomId,
            LocalDateTime orderAt
    ) {
        return new ChatRoomListResponse(
                roomId,
                "room-" + roomId,
                0,
                "preview",
                orderAt,
                orderAt,
                false
        );
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