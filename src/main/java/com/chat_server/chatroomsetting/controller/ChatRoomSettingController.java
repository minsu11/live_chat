package com.chat_server.chatroomsetting.controller;

import com.chat_server.chatlist.dto.reqeust.ChatRoomNotificationUpdateRequest;
import com.chat_server.chatlist.dto.response.ChatRoomNotificationUpdateResponse;
import com.chat_server.chatroom.dto.request.ChatRoomInviteMemberRequest;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.chatroommember.dto.response.ChatRoomMemberResponse;
import com.chat_server.chatroomsetting.dto.request.ChatRoomDisplayNameUpdateRequest;
import com.chat_server.chatroomsetting.dto.response.ChatRoomNameUpdateResponse;
import com.chat_server.chatroomsetting.service.ChatRoomSettingFacadeService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("${custom.api.common.prefix}${custom.api.chat-room.prefix}")
@RequiredArgsConstructor
public class ChatRoomSettingController {

    private final ChatRoomSettingFacadeService chatRoomSettingFacadeService;

    private final ChatRoomFacadeService chatRoomFacadeService;

    @PatchMapping("/{roomId}/settings/name")
    public ResponseEntity<ApiResponse<ChatRoomNameUpdateResponse>> updateRoomName(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long roomId,
            @RequestBody ChatRoomDisplayNameUpdateRequest request
            ){
        log.info("chat room setting controller start");
        Long userId = authenticatedUser.userId();
        ChatRoomNameUpdateResponse chatRoomNameUpdateResponse =
                chatRoomSettingFacadeService.updateChatRoomName(userId,roomId,request);
        ApiResponse<ChatRoomNameUpdateResponse> apiResponse = ApiResponse.success(200,"이름이 변경됐습니다.",chatRoomNameUpdateResponse);
        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/{roomId}/settings/notification")
    public ResponseEntity<ApiResponse<ChatRoomNotificationUpdateResponse>> updateNotification(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long roomId,
            @RequestBody ChatRoomNotificationUpdateRequest request
    ) {
        log.info("notification start");
        Long userId = authenticatedUser.userId();
        ChatRoomNotificationUpdateResponse response =
                chatRoomSettingFacadeService.updateNotification(roomId, userId, request);

        String message = response.muted() ? "알림이 꺼졌습니다." : "알림이 켜졌습니다.";
        return ResponseEntity.ok(ApiResponse.success(200, message, response));
    }

    @DeleteMapping("/{roomId}/settings/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ){
        log.info("leave room start");
        Long userId = authenticatedUser.userId();

        chatRoomSettingFacadeService.leaveRoom(roomId,userId);

        return ResponseEntity.ok(ApiResponse.success(200, "채팅방을 나갔습니다.", null));
    }
    @GetMapping("/{roomId}/settings/members")
    public ResponseEntity<ApiResponse<List<ChatRoomMemberResponse>>> getChatRoomMembers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long roomId
    ){
        log.info("chat room setting start");
        Long userId =  authenticatedUser.userId();
        List<ChatRoomMemberResponse> memberResponseList = chatRoomFacadeService.getChatroomMembers(roomId, userId);

        ApiResponse<List<ChatRoomMemberResponse>> response =
                ApiResponse.success(200, "채팅방 멤버 조회 성공", memberResponseList);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/settings/invite")
    public ResponseEntity<ApiResponse<Void>> inviteChatRoomMember(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long roomId,
            @RequestBody ChatRoomInviteMemberRequest chatRoomInviteMemberRequest
    ){
        log.info("chat room invite ");
        Long userId = authenticatedUser.userId();
        List<String> memberUuids = chatRoomInviteMemberRequest.memberUuids();
        chatRoomFacadeService.inviteMembers(roomId, userId, memberUuids);
        ApiResponse<Void> response = ApiResponse.success(200,"멤버가 초대됐습니다");
        return ResponseEntity.ok(response);
    }

}
