package com.chat_server.chatroom.repository;


import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chattype.entity.ChatType;

import java.util.Optional;

public interface ChatRoomRepositoryCustom {
    Optional<Long> findRoomIdByParticipantsHashAndChatType(String participantsHash, ChatType chatType);
    Optional<ChatRoomSummaryResponse> findChatRoomSummaryByRoomId(Long roomId);
}
