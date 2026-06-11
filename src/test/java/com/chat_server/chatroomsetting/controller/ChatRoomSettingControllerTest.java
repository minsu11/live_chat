package com.chat_server.chatroomsetting.controller;

import com.chat_server.chatlist.dto.reqeust.ChatRoomNotificationUpdateRequest;
import com.chat_server.chatlist.dto.response.ChatRoomNotificationUpdateResponse;
import com.chat_server.chatroom.dto.request.ChatRoomInviteMemberRequest;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.chatroommember.dto.response.ChatRoomMemberResponse;
import com.chat_server.chatroomsetting.dto.request.ChatRoomDisplayNameUpdateRequest;
import com.chat_server.chatroomsetting.dto.response.ChatRoomNameUpdateResponse;
import com.chat_server.chatroomsetting.service.ChatRoomSettingFacadeService;
import com.chat_server.user.dto.response.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatRoomSettingControllerTest {
    private final AuthenticatedUser user = new AuthenticatedUser(1L, "USER");

    @Test
    @DisplayName("채팅방 이름 변경 성공 시 변경 응답을 반환한다")
    void updateRoomNameShouldReturnUpdatedName() {
        ChatRoomSettingFacadeService settingService = mock(ChatRoomSettingFacadeService.class);
        ChatRoomFacadeService roomService = mock(ChatRoomFacadeService.class);
        ChatRoomSettingController controller = new ChatRoomSettingController(settingService, roomService);
        ChatRoomDisplayNameUpdateRequest request = new ChatRoomDisplayNameUpdateRequest("new");
        ChatRoomNameUpdateResponse data = new ChatRoomNameUpdateResponse(10L, "new");
        when(settingService.updateChatRoomName(1L, 10L, request)).thenReturn(data);

        assertThat(controller.updateRoomName(user, 10L, request).getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("알림 설정 변경 성공 시 muted 값에 맞는 메시지를 반환한다")
    void updateNotificationShouldReturnMutedResponse() {
        ChatRoomSettingFacadeService settingService = mock(ChatRoomSettingFacadeService.class);
        ChatRoomFacadeService roomService = mock(ChatRoomFacadeService.class);
        ChatRoomSettingController controller = new ChatRoomSettingController(settingService, roomService);
        ChatRoomNotificationUpdateRequest request = new ChatRoomNotificationUpdateRequest(true);
        ChatRoomNotificationUpdateResponse data = new ChatRoomNotificationUpdateResponse(10L, true);
        when(settingService.updateNotification(10L, 1L, request)).thenReturn(data);

        var response = controller.updateNotification(user, 10L, request);

        assertThat(response.getBody().getMessage()).isEqualTo("알림이 꺼졌습니다.");
        assertThat(response.getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("채팅방 나가기 성공 시 200 상태를 반환하고 서비스를 호출한다")
    void leaveRoomShouldReturnSuccess() {
        ChatRoomSettingFacadeService settingService = mock(ChatRoomSettingFacadeService.class);
        ChatRoomFacadeService roomService = mock(ChatRoomFacadeService.class);
        ChatRoomSettingController controller = new ChatRoomSettingController(settingService, roomService);

        assertThat(controller.leaveRoom(10L, user).getBody().getStatus()).isEqualTo(200);
        verify(settingService).leaveRoom(10L, 1L);
    }

    @Test
    @DisplayName("채팅방 멤버 조회 성공 시 멤버 목록을 반환한다")
    void getChatRoomMembersShouldReturnMembers() {
        ChatRoomSettingFacadeService settingService = mock(ChatRoomSettingFacadeService.class);
        ChatRoomFacadeService roomService = mock(ChatRoomFacadeService.class);
        ChatRoomSettingController controller = new ChatRoomSettingController(settingService, roomService);
        List<ChatRoomMemberResponse> data = List.of(new ChatRoomMemberResponse("u1", "me", null, true));
        when(roomService.getChatroomMembers(10L, 1L)).thenReturn(data);

        assertThat(controller.getChatRoomMembers(user, 10L).getBody().getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("채팅방 초대 성공 시 초대할 UUID 목록을 서비스로 전달한다")
    void inviteChatRoomMemberShouldDelegateMemberUuids() {
        ChatRoomSettingFacadeService settingService = mock(ChatRoomSettingFacadeService.class);
        ChatRoomFacadeService roomService = mock(ChatRoomFacadeService.class);
        ChatRoomSettingController controller = new ChatRoomSettingController(settingService, roomService);
        ChatRoomInviteMemberRequest request = new ChatRoomInviteMemberRequest(List.of("u2", "u3"));

        assertThat(controller.inviteChatRoomMember(user, 10L, request).getBody().getStatus()).isEqualTo(200);
        verify(roomService).inviteMembers(10L, 1L, List.of("u2", "u3"));
    }

    @Test
    @DisplayName("채팅방 설정 API 실패 시 서비스 예외를 전파한다")
    void shouldPropagateServiceException() {
        ChatRoomSettingFacadeService settingService = mock(ChatRoomSettingFacadeService.class);
        ChatRoomFacadeService roomService = mock(ChatRoomFacadeService.class);
        ChatRoomSettingController controller = new ChatRoomSettingController(settingService, roomService);
        doThrow(new IllegalStateException("leave failed")).when(settingService).leaveRoom(10L, 1L);

        assertThatThrownBy(() -> controller.leaveRoom(10L, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("leave failed");
    }
}
