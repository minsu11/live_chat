package com.chat_server.chatroom.exception;

import com.chat_server.common.dto.exception.NotFoundException;
import com.chat_server.error.enumulation.ErrorCode;

public class ChatRoomNotFoundException extends NotFoundException {
    /**
     * 채팅방 미존재 상황을 구체 메시지로 생성한다.
     *
     * @param message 프론트/로그에 전달할 상세 메시지
     */
    public ChatRoomNotFoundException(String message) {
        super(ErrorCode.CHAT_ROOM_NOT_FOUND, message);
    }

    /**
     * 채팅방 미존재 기본 예외를 생성한다.
     */
    public ChatRoomNotFoundException() {
        super(ErrorCode.CHAT_ROOM_NOT_FOUND, "chat room not found");
    }
}
