package com.chat_server.chatroom.controller;


import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${custom.api.common.prefix}${custom.api.chat-room.prefix}")
public class ChatRoomController {
    private final ChatRoomFacadeService chatRoomFacadeService;

    // todo 채팅방 목록 가지고 오기


    // todo 채팅방 생성
    @GetMapping("{userId}/register")
    public ResponseEntity<ApiResponse<Void>> createChatRoom(
            @PathVariable(name="userId") String friendId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ){

        log.info("chat room register");
        Long userId = authenticatedUser.userId();
        log.info("user id {}", userId);
        log.info("friend id {}", friendId);
        log.info("service before");
        chatRoomFacadeService.createOneToOneChatRoom(userId,friendId);
        ApiResponse<Void> apiResponse = ApiResponse.success(201, "1 대 1 대화창 생성");
        log.info("api response {}", apiResponse);

        return ResponseEntity.ok(apiResponse);
    }

    // todo 채팅방 나가기(채팅방 삭제)

    // todo 채팅방 설정 업데이트(해당 업데이트는 실제로 업데이트 할 예정)


}
