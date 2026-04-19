package com.chat_server.chatroomsetting.controller;

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

@Slf4j
@RestController
@RequestMapping("${custom.api.common.prefix}${custom.api.chat-room.prefix}")
@RequiredArgsConstructor
public class ChatRoomSettingController {

    private final ChatRoomSettingFacadeService chatRoomSettingFacadeService;

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

}
