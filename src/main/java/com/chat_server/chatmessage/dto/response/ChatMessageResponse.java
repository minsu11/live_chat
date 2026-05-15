package com.chat_server.chatmessage.dto.response;

import java.time.LocalDateTime;

/**
 * 웹소켓 브로드캐스트용 메시지 응답 DTO.
 *
 * @param messageId 메시지 ID
 * @param roomId 채팅방 ID
 * @param clientMessageId 클라이언트가 생성한 메시지 식별자. 없으면 null.
 * @param messageType 메시지 타입
 * @param sender 발신자 정보 DTO
 * @param content 메시지 내용
 * @param createdAt 메시지 생성 시각
 * @param mine 수신자 기준 본인 메시지 여부
 * @param unreadCount 해당 메시지를 아직 읽지 않은 멤버 수
 */
public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        String clientMessageId,
        String messageType,
        ChatMessageSenderResponse sender,
        String content,
        LocalDateTime createdAt,
        boolean mine,
        Integer unreadCount
) {

}