package com.chat_server.chatroom.service;

import com.chat_server.chatmessage.dto.response.ChatMessageCatchUpResponse;
import com.chat_server.chatroom.dto.request.CreateGroupChatRoomRequest;
import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.dto.response.CreateChatRoomResponse;

public interface ChatRoomFacadeService {

    /**
     * 채팅방 summary 정보를 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 유저 ID
     * @return 채팅방 요약 정보
     */
    ChatRoomSummaryResponse getChatRoomSummary(Long roomId, Long userId);

    /**
     * 채팅방 진입 시 필요한 메타데이터 + 메시지 페이지를 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 유저 ID
     * @param cursor 메시지 커서(없으면 첫 페이지)
     * @param limit 페이지 크기
     * @return 채팅방 진입 응답 DTO
     */
    ChatRoomEnterResponse enterChatRoom(Long roomId, Long userId, String cursor, int limit);

    /**
     * 1:1 채팅방을 조회하거나 없으면 생성한다.
     *
     * @param userId 요청 유저 ID
     * @param friendUuid 상대 유저 UUID
     * @return 채팅방 결과(방 ID/신규생성 여부)
     */
    ChatRoomResult getOrCreateOneToOneChatRoom(Long userId, String friendUuid);

    ChatRoomEnterResponse getChatRoomMessages(Long roomId, Long userId, String cursor, int limit);

    CreateChatRoomResponse createGroupChatRoom(Long requesterUserId, CreateGroupChatRoomRequest request);

    ChatMessageCatchUpResponse getMessagesAfter(Long roomId, Long userId, Long afterMessageId, int limit);
}
