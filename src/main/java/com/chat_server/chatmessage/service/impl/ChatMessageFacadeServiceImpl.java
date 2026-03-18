package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.userblock.service.UserBlockService;
import com.chat_server.websocket.broadcaster.ChatMessageBroadCaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * chat message facade message service
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageFacadeServiceImpl implements ChatMessageFacadeService {

    private final ChatMessageBroadCaster chatMessageBroadCaster;
    private final ChatRoomQueryService chatRoomQueryService;
    private final UserBlockService userBlockService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final ChatListService chatListService;


    @Override
    public void sendMessage(ChatSendRequest request, Long userId) {
        // todo 현재는 1 대 1 채팅방 보내기만 됨

        Long roomId = request.roomId();
        String messageType = request.messageType();
        String message = request.text();
        // 1. 방 + 타입 조회 (예: DM / GROUP / OPEN)
        //    - 존재 여부 확인
        //    - roomType 같이 가져오기
        ChatRoom room = chatRoomQueryService.getRoomOrThrow(roomId);

        // 채팅방 상대방 유저 아이디 가지고 오기.
        Long memberId = chatRoomQueryService.getMemberId(room.getId(),userId);
        // 2. 전송 권한 / 차단 여부 검증
        //    - 해당 방의 멤버인지?
        //    - 상대가 나를 차단했는지?
        //    - 방이 이미 종료/잠금 상태인지?
        validateSendPermission(room, userId, memberId, request);
        log.info("content : {}", request.text());
        // 3. 메시지 엔티티 생성 + 저장
        //    - content, senderId, roomId, messageType, createdAt..
        // todo message type 별로 공통 처리 구문
        ChatMessage chatMessage = chatMessageService.createChatMessage(room, userId, messageType, message);

        // 4. 부가 상태 업데이트
        //    - ChatRoom.lastMessageAt, lastMessagePreview
        //    - ChatList.unreadCount 증가
        //    - 필요하면 “읽음 정보” 초기화
        updateRoomAndChatListOnSend(room, chatMessage);
        chatListService.increaseUnreadCount(roomId,userId);
        // todo 임시 주석 처리, message read table 삭제 됨
//        chatMessageReadService.updateChatMessageRead(roomId, userId, chatMessage);

        // 5. 브로드캐스트 (WebSocket)
        //    - /sub/chat/rooms/{roomId} 같은 경로로 DTO 날리기
        // ChatMessageResponse dto = ChatMessageResponse.from(message);
        // messagingTemplate.convertAndSend("/sub/chat/rooms/" + room.getId(), dto);
        //   → 나중에 ChatMessageBroadcaster로 분리 가능
        // 브로드 캐스트

        Long messageId = chatMessage.getId();
        String messageContent = chatMessage.getMessageContent();
        LocalDateTime createdAt = chatMessage.getCreatedAt();

        ChatMessageResponse response = new ChatMessageResponse(
                messageId, roomId, userId, "",messageContent,createdAt);

        chatMessageBroadCaster.broadcastMessage(response);

        // 6. (선택) 서버에서 클라이언트로 “나에게도 에코”를 보낼지 여부
        //    - 클라이언트가 낙관적 UI로 먼저 그리면 굳이 안 보내도 됨
    }

     private void validateSendPermission(ChatRoom room, Long userId, Long memberId, ChatSendRequest request) {
        // 유효성 검사
        // todo 레포지토리 통해서 validation하는 것
         // 내 자신이 room member가 맞는지 판별
        chatRoomQueryService.validateMemberOrThrow(room.getId(), userId);

         // 상대가 차단 했는지 판별
        userBlockService.validateSenderNotBlocked(userId,memberId);

        // todo open chat 기능 개발 시 오픈채팅방 잠금 등의 유효성 검사 추가
     }
     private void updateRoomAndChatListOnSend(ChatRoom room, ChatMessage chatMessage) {
        chatRoomService.updateLastMessageInfo(room, chatMessage);

     }
}
