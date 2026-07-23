package com.chat_server.chatroomsetting.service.impl;

import com.chat_server.chatlist.dto.reqeust.ChatRoomNotificationUpdateRequest;
import com.chat_server.chatlist.dto.response.ChatRoomNotificationUpdateResponse;
import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.enums.MessageType;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.chatroomsetting.dto.request.ChatRoomDisplayNameUpdateRequest;
import com.chat_server.chatroomsetting.dto.response.ChatRoomNameUpdateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatRoomSettingFacadeServiceImplTest {

    private ChatListService chatListService;
    private ChatRoomService chatRoomService;
    private ChatMessageFacadeService messageFacadeService;
    private ChatRoomMemberService chatRoomMemberService;
    private ChatRoomSettingFacadeServiceImpl facade;

    @BeforeEach
    void setUp() {
        chatListService = mock(ChatListService.class);
        chatRoomService = mock(ChatRoomService.class);
        messageFacadeService = mock(ChatMessageFacadeService.class);
        chatRoomMemberService = mock(ChatRoomMemberService.class);
        facade = new ChatRoomSettingFacadeServiceImpl(
                chatListService,
                chatRoomService,
                messageFacadeService,
                chatRoomMemberService
        );
    }

    /**
     * 사용자별 채팅방 이름 변경 결과를 ChatList의 최종 customName 기준으로 응답하는지 검증한다.
     */
    @Test
    @DisplayName("채팅방 이름 변경 성공 - 저장된 사용자별 커스텀 이름을 응답한다")
    void updateChatRoomNameShouldReturnSavedCustomName() {
        ChatList updatedChatList = ChatList.builder().unreadCount(0).build();
        updatedChatList.updateCustomName("프로젝트 채팅방");
        ChatRoomDisplayNameUpdateRequest request =
                new ChatRoomDisplayNameUpdateRequest("프로젝트 채팅방");

        when(chatListService.updateCustomRoomName(10L, 100L, "프로젝트 채팅방"))
                .thenReturn(updatedChatList);

        ChatRoomNameUpdateResponse result = facade.updateChatRoomName(10L, 100L, request);

        assertThat(result.roomId()).isEqualTo(100L);
        assertThat(result.displayName()).isEqualTo("프로젝트 채팅방");
        verify(chatListService).updateCustomRoomName(10L, 100L, "프로젝트 채팅방");
    }

    /**
     * 커스텀 이름 저장 중 예외가 발생하면 성공 응답을 만들지 않고 예외를 그대로 전달하는지 검증한다.
     */
    @Test
    @DisplayName("채팅방 이름 변경 실패 - 저장 서비스 예외를 그대로 전달한다")
    void updateChatRoomNameShouldPropagateServiceException() {
        ChatRoomDisplayNameUpdateRequest request =
                new ChatRoomDisplayNameUpdateRequest("실패할 이름");
        when(chatListService.updateCustomRoomName(10L, 100L, "실패할 이름"))
                .thenThrow(new IllegalStateException("채팅방 이름 저장 실패"));

        assertThatThrownBy(() -> facade.updateChatRoomName(10L, 100L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("채팅방 이름 저장 실패");

        verifyNoInteractions(chatRoomService, chatRoomMemberService, messageFacadeService);
    }

    /**
     * 알림 음소거 설정을 변경한 뒤 Entity의 최종 muted 상태를 응답하는지 검증한다.
     */
    @Test
    @DisplayName("채팅방 알림 변경 성공 - 저장된 음소거 상태를 응답한다")
    void updateNotificationShouldReturnSavedMutedStatus() {
        ChatList updatedChatList = ChatList.builder().unreadCount(0).build();
        updatedChatList.updateMuted(true);
        ChatRoomNotificationUpdateRequest request =
                new ChatRoomNotificationUpdateRequest(true);

        when(chatListService.updateMutedStatus(100L, 10L, true))
                .thenReturn(updatedChatList);

        ChatRoomNotificationUpdateResponse result =
                facade.updateNotification(100L, 10L, request);

        assertThat(result.roomId()).isEqualTo(100L);
        assertThat(result.muted()).isTrue();
        verify(chatListService).updateMutedStatus(100L, 10L, true);
    }

    /**
     * 방 나가기 작업이 목록 제거, 멤버 비활성화, 참여자 수 감소,
     * 시스템 메시지 저장 및 전파 순서로 실행되는지 검증한다.
     */
    @Test
    @DisplayName("채팅방 나가기 성공 - 관련 상태 변경 후 퇴장 시스템 메시지를 전파한다")
    void leaveRoomShouldExecuteAllStepsInOrder() {
        facade.leaveRoom(100L, 10L);

        InOrder inOrder = inOrder(
                chatListService,
                chatRoomMemberService,
                chatRoomService,
                messageFacadeService
        );
        inOrder.verify(chatListService).leaveChatRoom(100L, 10L);
        inOrder.verify(chatRoomMemberService).leaveRoomMember(100L, 10L);
        inOrder.verify(chatRoomService).decrementParticipantCount(100L);
        inOrder.verify(messageFacadeService).saveAndBroadcastSystemMessage(
                100L,
                10L,
                MessageType.SYSTEM_LEAVE,
                "님이 나갔습니다."
        );
    }

    /**
     * 채팅 목록 제거 단계에서 실패하면 멤버 상태나 참여자 수를 추가로 변경하지 않는지 검증한다.
     */
    @Test
    @DisplayName("채팅방 나가기 실패 - 채팅 목록 제거 실패 시 후속 상태 변경을 중단한다")
    void leaveRoomShouldStopWhenChatListRemovalFails() {
        doThrow(new IllegalStateException("채팅 목록 제거 실패"))
                .when(chatListService).leaveChatRoom(100L, 10L);

        assertThatThrownBy(() -> facade.leaveRoom(100L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("채팅 목록 제거 실패");

        verifyNoInteractions(chatRoomMemberService, chatRoomService, messageFacadeService);
    }

    /**
     * 멤버 비활성화 단계에서 실패하면 참여자 수 감소와 시스템 메시지 전파를 실행하지 않는지 검증한다.
     */
    @Test
    @DisplayName("채팅방 나가기 실패 - 멤버 처리 실패 시 참여자 수와 시스템 메시지를 변경하지 않는다")
    void leaveRoomShouldStopWhenMemberLeaveFails() {
        doThrow(new IllegalStateException("멤버 퇴장 처리 실패"))
                .when(chatRoomMemberService).leaveRoomMember(100L, 10L);

        assertThatThrownBy(() -> facade.leaveRoom(100L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("멤버 퇴장 처리 실패");

        verify(chatListService).leaveChatRoom(100L, 10L);
        verify(chatRoomService, never()).decrementParticipantCount(anyLong());
        verifyNoInteractions(messageFacadeService);
    }
}
