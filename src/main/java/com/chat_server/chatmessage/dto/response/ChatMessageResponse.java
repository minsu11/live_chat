package com.chat_server.chatmessage.dto.response;

import java.time.LocalDateTime;

/**
 * 웹소켓 브로드캐스트용 메시지 응답 DTO.
 *
 * @param messageId 메시지 ID
 * @param roomId 채팅방 ID
 * @param sender 발신자 정보 DTO
 * @param content 메시지 내용
 * @param createdAt 메시지 생성 시각
 */
public record ChatMessageResponse(
    Long messageId,
    Long roomId,
    String messageType,
    ChatMessageSenderResponse sender,
    String content,
    LocalDateTime createdAt
) {

}
