package com.chat_server.chatmessage.service;

import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import org.springframework.data.domain.Slice;
import org.springframework.lang.Nullable;

public interface ChatMessageService {

    /**
     * 채팅 메시지를 생성/저장한다.
     *
     * @param chatRoom 메시지를 저장할 채팅방
     * @param userId 발신자 ID
     * @param messageType 메시지 타입(TEXT/IMAGE/FILE 등)
     * @param text 메시지 본문
     * @return 저장된 ChatMessage 엔티티
     *
     * <p>예외 상황:
     * <ul>
     *   <li>발신자 유저가 존재하지 않으면 UserNotFoundException</li>
     * </ul>
     */
    ChatMessage createChatMessage(ChatRoom chatRoom, Long userId, String messageType, String text);

    /**
     * 채팅방 진입 시 사용할 커서 기반 메시지 페이지를 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param limit 페이지 크기
     * @param cursorKey 커서 키(없으면 첫 페이지)
     * @return 메시지 Slice(다음 페이지 존재 여부 포함)
     */
    Slice<ChatMessageItemResponse> getEnterMessagesByCursor(Long roomId, int limit,
                                                            @Nullable ChatMessageCursorKey cursorKey);
}
