package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageSenderResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.user.service.UserDisplayNameService;
import com.chat_server.userblock.service.UserBlockService;
import com.chat_server.websocket.broadcaster.ChatMessageBroadCaster;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserDisplayNameService userDisplayNameService;

    @Override
    /**
     * 메시지 전송 전체 플로우를 오케스트레이션한다.
     *
     * <p>동작:
     * <ul>
     *   <li>채팅방/상대 멤버 조회</li>
     *   <li>전송 권한/차단 검증</li>
     *   <li>메시지 저장 및 방/목록 메타 업데이트</li>
     *   <li>수신자별 display nickname/mine 반영 응답 생성 후 사용자별 브로드캐스트</li>
     * </ul>
     *
     * @param request 메시지 전송 요청 DTO
     * @param userId 발신자 사용자 ID
     * @throws RuntimeException 채팅방/멤버 미존재, 권한 오류, 차단 관계 등 도메인 예외 발생 가능
     */
    public void sendMessage(ChatSendRequest request, Long userId) {
        // 메서드 시작 로그는 간단하게 info로 남긴다.
        log.info("sendMessage 호출");
        // 상세 파라미터는 debug에만 남겨 운영 로그 노이즈를 줄인다.
        log.debug("sendMessage params - request: {}, userId: {}", request, userId);
        Long roomId = request.roomId();
        String messageType = request.messageType();
        String message = request.text();

        ChatRoom room = chatRoomQueryService.getRoomOrThrow(roomId);
        Long memberId = chatRoomQueryService.getMemberId(room.getId(), userId);

        validateSendPermission(room, userId, memberId, request);
        ChatMessage chatMessage = chatMessageService.createChatMessage(room, userId, messageType, message);

        updateRoomAndChatListOnSend(room, chatMessage);
        chatListService.increaseUnreadCount(roomId, userId);

        // 채팅방 멤버 목록을 조회하여 사용자별(수신자별) payload를 생성/전송한다.
        List<Long> roomMemberUserIds = chatListService.getRoomMemberUserIds(roomId);
        log.debug("sendMessage roomMemberUserIds: {}", roomMemberUserIds);
        for (Long receiverUserId : roomMemberUserIds) {
            ChatMessageResponse response = createResponseForReceiver(chatMessage, roomId, userId, receiverUserId);
            chatMessageBroadCaster.broadcastMessage(receiverUserId, response);
            log.debug("sendMessage broadcast 완료 - receiverUserId: {}, response: {}", receiverUserId, response);
        }
        log.debug("sendMessage 완료 - roomId: {}, messageId: {}", roomId, chatMessage.getId());
    }

    /**
     * 발신자의 전송 가능 여부를 검증한다.
     *
     * @param room 메시지를 보내려는 채팅방
     * @param userId 발신자 사용자 ID
     * @param memberId 상대 멤버 사용자 ID
     * @param request 원본 전송 요청(디버그 추적용)
     * @throws RuntimeException 멤버십 없음/차단됨 등 검증 실패 시 도메인 예외
     */
    private void validateSendPermission(ChatRoom room, Long userId, Long memberId, ChatSendRequest request) {
        // 송신자 권한 및 차단 상태를 검증한다.
        log.info("validateSendPermission 호출");
        log.debug("validateSendPermission params - roomId: {}, userId: {}, memberId: {}, request: {}",
                room.getId(), userId, memberId, request);
        chatRoomQueryService.validateMemberOrThrow(room.getId(), userId);
        userBlockService.validateSenderNotBlocked(userId, memberId);
        log.debug("validateSendPermission 완료 - roomId: {}, userId: {}", room.getId(), userId);
    }

    /**
     * 메시지 저장 후 채팅방의 마지막 메시지 메타 정보를 동기화한다.
     *
     * @param room 대상 채팅방 엔티티
     * @param chatMessage 방금 저장한 메시지 엔티티
     */
    private void updateRoomAndChatListOnSend(ChatRoom room, ChatMessage chatMessage) {
        // 채팅방 마지막 메시지 정보를 동기화한다.
        log.info("updateRoomAndChatListOnSend 호출");
        log.debug("updateRoomAndChatListOnSend params - roomId: {}, chatMessageId: {}", room.getId(), chatMessage.getId());
        chatRoomService.updateLastMessageInfo(room, chatMessage);
        log.debug("updateRoomAndChatListOnSend 완료 - roomId: {}, lastMessageId: {}", room.getId(), chatMessage.getId());
    }

    /**
     * 수신자 기준 display nickname 우선순위(친구 커스텀 닉네임 -> 사용자 기본 닉네임)를 반영한다.
     *
     * @param chatMessage 저장된 메시지 엔티티
     * @param roomId 메시지가 속한 채팅방 ID
     * @param senderId 발신자 사용자 ID
     * @param receiverUserId 현재 응답을 만들 수신자 사용자 ID
     * @return 수신자 관점(표시 닉네임/mine)이 반영된 채팅 응답 DTO
     */
    private ChatMessageResponse createResponseForReceiver(
            ChatMessage chatMessage,
            Long roomId,
            Long senderId,
            Long receiverUserId
    ) {
        log.info("createResponseForReceiver 호출");
        log.debug("createResponseForReceiver params - chatMessageId: {}, roomId: {}, senderId: {}, receiverUserId: {}",
                chatMessage.getId(), roomId, senderId, receiverUserId);

        // 수신자별 display nickname을 계산한다.
        String displayNickname = userDisplayNameService
                .resolveDisplayName(senderId, receiverUserId)
                .orElse(chatMessage.getSender().getNickname());
        boolean mine = senderId.equals(receiverUserId);

        ChatMessageSenderResponse sender = new ChatMessageSenderResponse(
                senderId,
                chatMessage.getSender().getUuid(),
                displayNickname,
                null,
                mine
        );

        ChatMessageResponse response = new ChatMessageResponse(
                chatMessage.getId(),
                roomId,
                sender,
                chatMessage.getMessageContent(),
                chatMessage.getCreatedAt()
        );
        log.debug("createResponseForReceiver return - response: {}", response);
        return response;
    }
}
