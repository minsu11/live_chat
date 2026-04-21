package com.chat_server.chatroom.controller;


import com.chat_server.chatroom.dto.request.CreateGroupChatRoomRequest;
import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.dto.response.CreateChatRoomResponse;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import jakarta.validation.Valid;
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

    /**
     * 채팅방 진입 정보를 조회한다.
     *
     * @param authenticatedUser 인증 유저
     * @param roomId 채팅방 ID
     * @param cursor 커서(없으면 첫 페이지)
     * @param limit 페이지 크기
     * @return 채팅방 메타데이터 + 메시지 목록 + nextCursor
     *
     * <p>예외 상황:
     * <ul>
     *   <li>해당 유저가 방 멤버가 아니면 예외</li>
     *   <li>roomId가 유효하지 않으면 예외</li>
     * </ul>
     */
    @GetMapping("/{roomId}/enter")
    public ResponseEntity<ApiResponse<ChatRoomEnterResponse>> enterChatRoom(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long roomId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit
    ) {
        log.info("채팅방 입장");
        Long userId = authenticatedUser.userId();
        ChatRoomEnterResponse chatRoomEnterResponse = chatRoomFacadeService.enterChatRoom(roomId, userId, cursor, limit);
        ApiResponse<ChatRoomEnterResponse> response = ApiResponse.success(200, "채팅방 진입 정보 조회 성공", chatRoomEnterResponse);
        log.info("채팅방 입장 컨트롤러 종료");
        return ResponseEntity.ok(response);
    }

    /**
     * 채팅방 summary 정보를 조회한다.
     *
     * @param authenticatedUser 인증 유저
     * @param roomId 채팅방 ID
     * @return 채팅방 요약 정보
     */
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

    /**
     * 1:1 채팅방을 생성하거나 기존 방을 조회한다.
     *
     * @param friendId 상대 유저 UUID
     * @param authenticatedUser 인증 유저
     * @return 채팅방 결과(방 ID/신규생성 여부)
     */
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

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatRoomEnterResponse>> getChatRoomMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("chat room enter");

        ChatRoomEnterResponse response =
                chatRoomFacadeService.getChatRoomMessages(roomId, user.userId(), cursor, limit);
        ApiResponse<ChatRoomEnterResponse> apiResponse = ApiResponse.success(200,"대화 메세지 업데이트",response);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("group")
    public ResponseEntity<ApiResponse<CreateChatRoomResponse>> createGroupChatRoom(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody @Valid CreateGroupChatRoomRequest request
    ){
        log.info("create group start");
        Long userId = user.userId();
        CreateChatRoomResponse response = chatRoomFacadeService.createGroupChatRoom(
                userId,
                request
        );
        ApiResponse<CreateChatRoomResponse> apiResponse = ApiResponse.success(201,"그룹방이 만들어졌습니다.",response);
        return ResponseEntity.ok(apiResponse);
    }
}
