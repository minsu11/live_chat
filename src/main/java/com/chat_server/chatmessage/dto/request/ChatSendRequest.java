package com.chat_server.chatmessage.dto.request;

import com.chat_server.chatmessage.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


/**
 * 채팅 메시지 전송 요청 DTO.
 *
 * @param roomId 메시지를 전송할 채팅방 ID
 * @param messageType 메시지 타입
 * @param messageContent 메시지 내용
 * @param clientMessageId 클라이언트가 생성한 메시지 식별자.
 *                        재시도/중복 방지/idempotency/부하 테스트 검증에 사용할 수 있다.
 */
public record ChatSendRequest (@NotNull Long roomId,
                               @NotNull MessageType messageType,
                               @NotBlank String messageContent,
                                String clientMessageId
){
}
