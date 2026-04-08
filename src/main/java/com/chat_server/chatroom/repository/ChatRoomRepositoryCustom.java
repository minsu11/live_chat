package com.chat_server.chatroom.repository;


import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.enums.RoomType;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepositoryCustom {
    Optional<Long> findRoomIdByDmKeyAndRoomType(String dmKey, RoomType roomType);
    Optional<ChatRoomSummaryResponse> findChatRoomSummaryByRoomId(Long roomId, Long userId);
    Optional<Long> findMemberIdByRoomId(Long roomId, Long userId);
    List<Long> findOtherMemberIdsByRoomId(Long roomId, Long userId);
    Long countMembersByRoomId(Long roomId);
    boolean existsByUserId(Long roomId, Long userId);
}
