package com.chat_server.chatmessage.service.impl;

import com.chat_server.chatlist.dto.event.ChatListUpsertEvent;
import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.request.ChatSendRequest;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatnotification.dto.event.ChatNotificationEvent;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.resolver.ChatRoomDisplayResolver;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.common.mapper.ChatListUpsertEventMapper;
import com.chat_server.common.mapper.ChatMessageResponseMapper;
import com.chat_server.common.mapper.ChatNotificationEventMapper;
import com.chat_server.user.service.UserDisplayNameService;
import com.chat_server.userblock.service.UserBlockService;
import com.chat_server.userprofileImage.service.UserProfileImageService;
import com.chat_server.websocket.broadcaster.chatmessage.ChatListEventBroadcaster;
import com.chat_server.websocket.broadcaster.chatmessage.ChatMessageBroadCaster;

import java.util.List;
import java.util.Map;

import com.chat_server.websocket.broadcaster.chatmessage.ChatNotificationBroadcaster;
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
    private final ChatMessageResponseMapper chatMessageResponseMapper;
    private final ChatMessageBroadCaster chatMessageBroadCaster;
    private final ChatRoomQueryService chatRoomQueryService;
    private final UserBlockService userBlockService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final ChatListService chatListService;
    private final UserDisplayNameService userDisplayNameService;
    private final UserProfileImageService userProfileImageService;
    private final ChatNotificationBroadcaster chatNotificationBroadcaster;
    private final ChatNotificationEventMapper chatNotificationEventMapper;
    private final ChatRoomDisplayResolver chatRoomDisplayResolver;
    private final ChatListEventBroadcaster chatListEventBroadcaster;
    private final ChatListUpsertEventMapper chatListUpsertEventMapper;

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
    @Override
    public void sendMessage(ChatSendRequest request, Long userId) {
        // 메서드 시작 로그는 간단하게 info로 남긴다.
        log.info("sendMessage 호출");
        // 상세 파라미터는 debug에만 남겨 운영 로그 노이즈를 줄인다.
        log.debug("sendMessage params - request: {}, userId: {}", request, userId);
        Long roomId = request.roomId();
        String messageType = request.messageType().name();
        String message = request.messageContent();

        // chatting room
        ChatRoom room = chatRoomQueryService.getRoomOrThrow(roomId);
        String memberProfileUrl = userProfileImageService.getUserProfileUrl(userId);
        // 발신자의 전송 권한
        validateSendPermission(room, userId);
        ChatMessage chatMessage = chatMessageService.createChatMessage(room, userId, messageType, message);

        updateRoomAndChatListOnSend(room, chatMessage);
        chatListService.increaseUnreadCount(roomId, userId);
        chatListService.markSenderAsReadOnSend(roomId,userId,chatMessage.getId());


        // 채팅방 멤버 목록을 조회하여 사용자별(수신자별) payload를 생성/전송한다.
        List<Long> roomMemberUserIds = chatListService.getRoomMemberUserIds(roomId);
        log.debug("sendMessage roomMemberUserIds: {}", roomMemberUserIds);
        Map<Long, Integer> unreadCountMap = chatListService.getUnreadCountMap(roomId, roomMemberUserIds);
        int messageUnreadCount = Math.max(roomMemberUserIds.size() - 1, 0);
        for (Long receiverUserId : roomMemberUserIds) {

            // 🔥 2. 차단 체크 (핵심 추가)
            if (!userId.equals(receiverUserId) &&
                    userBlockService.isBlocked(userId, receiverUserId)) {
                log.debug("차단된 사용자 - sender: {}, receiver: {}", userId, receiverUserId);
                continue; // ❌ 이 사람한테는 안보냄
            }

            ChatMessageResponse response = createResponseForReceiver(
                    chatMessage,
                    roomId,
                    userId,
                    receiverUserId,
                    memberProfileUrl,
                    messageUnreadCount);
            log.info("broadcast room message. roomId={}, messageId={}", roomId, chatMessage.getId());
            chatMessageBroadCaster.broadcastMessage(receiverUserId, response);

            ChatListItemResponse chatListItem = chatListService.getChatListItem(roomId, receiverUserId);
            ChatListUpsertEvent chatListEvent =
                    chatListUpsertEventMapper.toChatListUpsertEvent(chatListItem);

            chatListEventBroadcaster.broadcastUpsertToUser(receiverUserId, chatListEvent);

            if (!userId.equals(receiverUserId)) {
                String title = chatRoomDisplayResolver.resolveTitle(roomId, receiverUserId, room);

                ChatNotificationEvent notificationEvent =
                        chatNotificationEventMapper.toChatNotificationEvent(
                                roomId,
                                title,
                                chatListItem.lastMessagePreview(),
                                chatMessage.getCreatedAt()
                        );

                chatNotificationBroadcaster.broadcastToUser(receiverUserId, notificationEvent);
            }
            log.debug("sendMessage broadcast 완료 - receiverUserId: {}, response: {}", receiverUserId, response);
        }
        log.debug("sendMessage 완료 - roomId: {}, messageId: {}", roomId, chatMessage.getId());
    }

    /**
     * 발신자의 전송 가능 여부를 검증한다.
     *
     * @param room 메시지를 보내려는 채팅방
     * @param userId 발신자 사용자 ID
     * @throws RuntimeException 멤버십 없음/차단됨 등 검증 실패 시 도메인 예외
     */
    private void validateSendPermission(ChatRoom room, Long userId) {
        // 송신자 권한 및 차단 상태를 검증한다.
        log.info("validateSendPermission 호출");
        log.debug("validateSendPermission params - roomId: {}, userId: {}",
                room.getId(), userId);
        chatRoomQueryService.validateMemberOrThrow(room.getId(), userId);
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
     * 저장된 메시지를 특정 수신자(receiverUserId) 관점의 실시간 브로드캐스트 응답 DTO로 생성한다.
     *
     * <p>수신자별로 다음 값이 달라질 수 있다.
     * <ul>
     *   <li>sender.senderNickname: 수신자 기준 표시 이름</li>
     *   <li>sender.mine: 수신자가 발신자인 경우 true, 아니면 false</li>
     * </ul>
     *
     * <p>발신자의 UUID, 프로필 이미지 URL, 메시지 내용, 생성 시각 등은 저장된 메시지 기준 값을 사용한다.</p>
     *
     * @param chatMessage 저장된 채팅 메시지 엔티티
     * @param roomId 채팅방 ID
     * @param senderId 발신자 사용자 ID
     * @param receiverUserId 현재 브로드캐스트를 수신할 사용자 ID
     * @param profileImageUrl 발신자 사용자 프로필 이미지 url
     * @return 수신자 관점이 반영된 실시간 메시지 응답 DTO
     */
    private ChatMessageResponse createResponseForReceiver(
            ChatMessage chatMessage,
            Long roomId,
            Long senderId,
            Long receiverUserId,
            String profileImageUrl,
            int unreadCount
    ) {
        log.info("createResponseForReceiver 호출");
        log.debug("createResponseForReceiver params - chatMessageId: {}, roomId: {}, senderId: {}, receiverUserId: {}",
                chatMessage.getId(), roomId, senderId, receiverUserId);

        // 수신자별 display nickname을 계산한다.
        String displayNickname = userDisplayNameService
                .resolveDisplayName(senderId, receiverUserId)
                .orElse(chatMessage.getSender().getNickname());

        ChatMessageResponse response = chatMessageResponseMapper.fromMessage(
                chatMessage.getId(),
                roomId,
                chatMessage.getMessageType().name(),
                senderId,
                chatMessage.getSender().getUuid(),
                displayNickname,
                profileImageUrl,
                chatMessage.getMessageContent(),
                chatMessage.getCreatedAt(),
                receiverUserId,
                unreadCount
        );
        log.debug("createResponseForReceiver return - response: {}", response);
        return response;
    }


}
