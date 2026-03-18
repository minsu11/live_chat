package com.chat_server.chatroom.repository;


import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.enums.RoomType;

import java.util.Optional;

public interface ChatRoomRepositoryCustom {
    Optional<Long> findRoomIdByDmKeyAndRoomType(String dmKey, RoomType roomType);
    Optional<ChatRoomSummaryResponse> findChatRoomSummaryByRoomId(Long roomId, Long userId);
    Optional<Long> findMemberIdByRoomId(Long roomId, Long userId);
    boolean existsByUserId(Long roomId, Long userId);
}
