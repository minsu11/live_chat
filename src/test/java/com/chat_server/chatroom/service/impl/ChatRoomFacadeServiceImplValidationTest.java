package com.chat_server.chatroom.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatread.service.ChatReadFacadeService;
import com.chat_server.chatroom.dto.request.CreateGroupChatRoomRequest;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.resolver.ChatRoomDisplayResolver;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.common.mapper.ChatListUpsertEventMapper;
import com.chat_server.common.mapper.ChatMessageResponseMapper;
import com.chat_server.redis.service.ChatMetadataRedisService;
import com.chat_server.user.entity.User;
import com.chat_server.user.service.UserDisplayNameService;
import com.chat_server.user.service.UserService;
import com.chat_server.websocket.broadcaster.chatmessage.ChatListEventBroadcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatRoomFacadeServiceImplValidationTest {

    private ChatRoomService chatRoomService;
    private ChatListService chatListService;
    private ChatRoomMemberService chatRoomMemberService;
    private ChatMessageService chatMessageService;
    private ChatRoomQueryService chatRoomQueryService;
    private UserService userService;
    private UserDisplayNameService userDisplayNameService;
    private ChatReadFacadeService chatReadFacadeService;
    private ChatListEventBroadcaster chatListEventBroadcaster;
    private ChatRoomFacadeServiceImpl target;

    @BeforeEach
    void setUp() {
        chatRoomService = mock(ChatRoomService.class);
        chatListService = mock(ChatListService.class);
        chatRoomMemberService = mock(ChatRoomMemberService.class);
        chatMessageService = mock(ChatMessageService.class);
        chatRoomQueryService = mock(ChatRoomQueryService.class);
        userService = mock(UserService.class);
        userDisplayNameService = mock(UserDisplayNameService.class);
        chatReadFacadeService = mock(ChatReadFacadeService.class);
        chatListEventBroadcaster = mock(ChatListEventBroadcaster.class);
        ChatRoomDisplayResolver chatRoomDisplayResolver = mock(ChatRoomDisplayResolver.class);
        ChatListUpsertEventMapper chatListUpsertEventMapper = mock(ChatListUpsertEventMapper.class);
        ChatMessageFacadeService chatMessageFacadeService = mock(ChatMessageFacadeService.class);
        ChatMetadataRedisService chatMetadataRedisService = mock(ChatMetadataRedisService.class);

        target = new ChatRoomFacadeServiceImpl(
                chatRoomService,
                chatListService,
                chatRoomMemberService,
                chatMessageService,
                chatRoomQueryService,
                userService,
                userDisplayNameService,
                chatReadFacadeService,
                chatListEventBroadcaster,
                chatRoomDisplayResolver,
                chatListUpsertEventMapper,
                chatMessageFacadeService,
                chatMetadataRedisService,
                new ObjectMapper(),
                new ChatMessageResponseMapper()
        );
    }

    /**
     * 그룹 채팅방 생성 요청 자체가 null인 validation 실패를 검증한다.
     * 채팅방 생성, 사용자 조회, 멤버십 생성이 시작되면 안 된다.
     */
    @Test
    @DisplayName("그룹 채팅방 validation 실패 - 요청 객체가 null이면 생성 흐름을 시작하지 않는다")
    void createGroupChatRoomShouldRejectNullRequest() {
        assertThatThrownBy(() -> target.createGroupChatRoom(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("그룹 채팅방 요청 값이 없습니다.");

        verifyNoInteractions(userService, chatRoomService, chatRoomMemberService,
                chatListService, chatListEventBroadcaster);
    }

    /**
     * 초대할 멤버 목록이 null인 validation 실패를 검증한다.
     * 사용자 UUID 변환과 채팅방 저장이 호출되지 않아야 한다.
     */
    @Test
    @DisplayName("그룹 채팅방 validation 실패 - 멤버 목록이 null이면 초대 대상 선택 예외가 발생한다")
    void createGroupChatRoomShouldRejectNullMemberList() {
        CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest("그룹방", null);

        assertThatThrownBy(() -> target.createGroupChatRoom(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("그룹 채팅방에 초대할 멤버를 선택해야 합니다.");

        verifyNoInteractions(userService, chatRoomService, chatRoomMemberService,
                chatListService, chatListEventBroadcaster);
    }

    /**
     * 빈 멤버 목록을 전달한 validation 실패를 검증한다.
     * 빈 요청으로 그룹 채팅방이 생성되지 않아야 한다.
     */
    @Test
    @DisplayName("그룹 채팅방 validation 실패 - 빈 멤버 목록이면 초대 대상 선택 예외가 발생한다")
    void createGroupChatRoomShouldRejectEmptyMemberList() {
        CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest("그룹방", List.of());

        assertThatThrownBy(() -> target.createGroupChatRoom(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("그룹 채팅방에 초대할 멤버를 선택해야 합니다.");

        verifyNoInteractions(userService, chatRoomService, chatRoomMemberService,
                chatListService, chatListEventBroadcaster);
    }

    /**
     * null, 공백, 중복 UUID를 제거한 뒤 유효한 UUID가 2개 미만인 validation 실패를 검증한다.
     * UUID 정규화 단계에서 실패하므로 사용자 조회가 발생하지 않아야 한다.
     */
    @Test
    @DisplayName("그룹 채팅방 validation 실패 - 공백과 중복을 제거한 유효 UUID가 2개 미만이면 생성하지 않는다")
    void createGroupChatRoomShouldRejectInsufficientNormalizedUuids() {
        CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(
                "그룹방",
                Arrays.asList(" user-2 ", "user-2", " ", null)
        );

        assertThatThrownBy(() -> target.createGroupChatRoom(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("그룹 채팅방은 본인을 제외한 2명 이상의 멤버가 필요합니다.");

        verifyNoInteractions(userService, chatRoomService, chatRoomMemberService,
                chatListService, chatListEventBroadcaster);
    }

    /**
     * 서로 다른 UUID가 같은 사용자 ID로 해석되는 경계 상황을 검증한다.
     * ID 중복 제거 후 본인을 제외한 대상이 2명 미만이면 방을 생성하면 안 된다.
     */
    @Test
    @DisplayName("그룹 채팅방 validation 실패 - UUID가 달라도 동일 사용자로 조회되어 최종 초대자가 2명 미만이면 생성하지 않는다")
    void createGroupChatRoomShouldRejectDuplicateResolvedUserIds() {
        CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(
                "그룹방",
                List.of("uuid-a", "uuid-b", "uuid-c")
        );
        when(userService.getUserIdByUserUuid("uuid-a")).thenReturn(2L);
        when(userService.getUserIdByUserUuid("uuid-b")).thenReturn(2L);
        when(userService.getUserIdByUserUuid("uuid-c")).thenReturn(1L);

        assertThatThrownBy(() -> target.createGroupChatRoom(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("그룹 채팅방은 본인을 제외한 2명 이상의 멤버가 필요합니다.");

        verify(chatRoomService, never()).createGroupChatRoom(any(), any());
        verifyNoInteractions(chatRoomMemberService, chatListService, chatListEventBroadcaster);
    }

    /**
     * 채팅방 저장 단계에서 예외가 발생한 실패 경로를 검증한다.
     * 방 ID가 생성되지 않았으므로 멤버십, 채팅 목록, 실시간 이벤트를 생성하면 안 된다.
     */
    @Test
    @DisplayName("그룹 채팅방 생성 실패 - 채팅방 저장 예외가 발생하면 멤버십과 목록 이벤트를 생성하지 않는다")
    void createGroupChatRoomShouldStopWhenRoomPersistenceFails() {
        CreateGroupChatRoomRequest request = new CreateGroupChatRoomRequest(
                "그룹방",
                List.of("uuid-2", "uuid-3")
        );
        when(userService.getUserIdByUserUuid("uuid-2")).thenReturn(2L);
        when(userService.getUserIdByUserUuid("uuid-3")).thenReturn(3L);
        RuntimeException persistenceFailure = new RuntimeException("room insert failed");
        when(chatRoomService.createGroupChatRoom("그룹방", 1L)).thenThrow(persistenceFailure);

        assertThatThrownBy(() -> target.createGroupChatRoom(1L, request))
                .isSameAs(persistenceFailure);

        verifyNoInteractions(chatRoomMemberService, chatListService, chatListEventBroadcaster);
    }

    /**
     * 1:1 채팅 상대 UUID 조회 중 예외가 발생하는 실패 경로를 검증한다.
     * 상대 사용자를 확인하지 못했으므로 방 생성과 양쪽 멤버십 보장을 시작하면 안 된다.
     */
    @Test
    @DisplayName("1대1 채팅방 생성 실패 - 상대 사용자 조회 예외가 발생하면 방과 멤버십을 생성하지 않는다")
    void getOrCreateOneToOneChatRoomShouldStopWhenFriendLookupFails() {
        RuntimeException userNotFound = new RuntimeException("user not found");
        when(userService.getUserIdByUserUuid("missing-uuid")).thenThrow(userNotFound);

        assertThatThrownBy(() -> target.getOrCreateOneToOneChatRoom(1L, "missing-uuid"))
                .isSameAs(userNotFound);

        verifyNoInteractions(chatRoomService, chatRoomMemberService, chatListService);
    }

    /**
     * 1:1 채팅방은 생성됐지만 요청자 chat_list 멤버십 생성에 실패하는 부분 실패 경로를 검증한다.
     * 이후 상대방 목록과 room_member 멤버십 생성은 진행하지 않아야 한다.
     */
    @Test
    @DisplayName("1대1 채팅방 생성 부분 실패 - 요청자 채팅 목록 생성이 실패하면 이후 멤버십 처리를 중단한다")
    void getOrCreateOneToOneChatRoomShouldStopWhenRequesterChatListMembershipFails() {
        when(userService.getUserIdByUserUuid("friend-uuid")).thenReturn(2L);
        when(chatRoomService.getOrCreateOneToOneChatRoom(1L, 2L))
                .thenReturn(new ChatRoomResult(100L, true));
        RuntimeException membershipFailure = new RuntimeException("chat list insert failed");
        doThrow(membershipFailure).when(chatListService).ensureMembership(100L, 1L);

        assertThatThrownBy(() -> target.getOrCreateOneToOneChatRoom(1L, "friend-uuid"))
                .isSameAs(membershipFailure);

        verify(chatListService, never()).ensureMembership(100L, 2L);
        verifyNoInteractions(chatRoomMemberService);
    }

    /**
     * 재연결 누락 메시지 조회에서 채팅방 조회가 실패하는 예외 경로를 검증한다.
     * 실제 메시지 조회와 표시 이름 계산이 호출되지 않아야 한다.
     */
    @Test
    @DisplayName("재연결 복구 실패 - 채팅방이 존재하지 않으면 누락 메시지 조회와 표시 이름 계산을 중단한다")
    void getMessagesAfterShouldStopWhenRoomLookupFails() {
        RuntimeException roomNotFound = new RuntimeException("chat room not found");
        when(chatRoomQueryService.getRoomOrThrow(10L)).thenThrow(roomNotFound);

        assertThatThrownBy(() -> target.getMessagesAfter(10L, 1L, 100L, 50))
                .isSameAs(roomNotFound);

        verify(chatRoomQueryService).validateMemberOrThrow(10L, 1L);
        verifyNoInteractions(chatMessageService, userDisplayNameService);
    }

    /**
     * 재연결 누락 메시지 조회 중 Repository/Service 예외가 발생하는 경로를 검증한다.
     * 메시지 결과가 없으므로 표시 이름 계산을 실행하지 않아야 한다.
     */
    @Test
    @DisplayName("재연결 복구 실패 - 누락 메시지 조회 예외가 발생하면 응답 변환과 표시 이름 계산을 중단한다")
    void getMessagesAfterShouldStopWhenMessageLookupFails() {
        ChatRoom room = ChatRoom.builder()
                .id(10L)
                .roomType(RoomType.GROUP)
                .name("room")
                .createdBy(User.builder().id(1L).uuid("creator").nickname("creator").build())
                .build();
        when(chatRoomQueryService.getRoomOrThrow(10L)).thenReturn(room);
        RuntimeException queryFailure = new RuntimeException("message query failed");
        when(chatMessageService.getMessagesAfter(10L, 100L, 100)).thenThrow(queryFailure);

        assertThatThrownBy(() -> target.getMessagesAfter(10L, 1L, 100L, 999))
                .isSameAs(queryFailure);

        verifyNoInteractions(userDisplayNameService);
    }
}
