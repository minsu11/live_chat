package com.chat_server.chatroom.service;

import com.chat_server.chatroom.entity.ChatRoom;

/**
 * 채팅방 조회 및 채팅방 유효성 검증 도메인 서비스. 컨트롤러에 직접적으로 사용 금지
 */
public interface ChatRoomQueryService {
    ChatRoom getRoomOrThrow(Long roomId);
}
