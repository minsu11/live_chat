package com.chat_server.chatroom.controller;


import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${custom.api.common.prefix}${custom.api.chat-room.prefix}")
public class ChatRoomController {
    private final ChatRoomFacadeService chatRoomFacadeService;
    // todo 채팅방 정보
    @GetMapping("/{roomId}/enter")
    public ResponseEntity<ApiResponse<ChatRoomEnterResponse>> enterChatRoom(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long roomId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit
    ) {
        Long userId = authenticatedUser.userId();
        ChatRoomEnterResponse chatRoomEnterResponse = chatRoomFacadeService.enterChatRoom(roomId, userId, cursor, limit);
        ApiResponse<ChatRoomEnterResponse> response = ApiResponse.success(200, "채팅방 진입 정보 조회 성공", chatRoomEnterResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}/summary")
    public ResponseEntity<ApiResponse<ChatRoomSummaryResponse>> getChatRoom(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long roomId
    ){
        log.info("chat room info get start");
        log.info("roon id : {}", roomId);
        Long userId = authenticatedUser.userId();
        log.info("user id : {}", userId);
        ChatRoomSummaryResponse chatRoomSummaryResponse = chatRoomFacadeService.getChatRoomSummary(roomId, userId);
        ApiResponse<ChatRoomSummaryResponse> response = ApiResponse.success(200,"채팅방 조회 성공", chatRoomSummaryResponse);
        log.info("chat room info get end");
        return ResponseEntity.ok(response);
    }

    @GetMapping("{userId}/register")
    public ResponseEntity<ApiResponse<ChatRoomResult>> createChatRoom(
            @PathVariable(name="userId") String friendId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ){
        log.info("chat room register");
        Long userId = authenticatedUser.userId();
        log.info("user id {}", userId);
        log.info("friend id {}", friendId);
        log.info("service before");
        ChatRoomResult chatRoomResult = chatRoomFacadeService.getOrCreateOneToOneChatRoom(userId,friendId);
        ApiResponse<ChatRoomResult> apiResponse = ApiResponse.success(201, "1 대 1 대화창 생성", chatRoomResult);
        log.info("api response {}", apiResponse);

        return ResponseEntity.ok(apiResponse);
    }

    // todo 채팅방 나가기(채팅방 삭제)
    @DeleteMapping("remove")
    public ResponseEntity<ApiResponse<Void>> removeChatRoom(){
        // soft delete 진행 예정, 해당 컨트롤러는 1대1 그룹, 오픈 채팅 나가기

        return null;
    }

    // todo 채팅방 설정 업데이트(해당 업데이트는 실제로 업데이트 할 예정)


}
