package com.chat_server.chatlist.repository;

import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.dto.response.ChatRoomListRow;
import com.chat_server.common.cursor.ChatListCursorKey;
import com.chat_server.common.cursor.CursorKey;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

public interface ChatListRepositoryCustom {
    Slice<ChatRoomListResponse> getChatRoomListByCursor(Long userId, int limit, ChatListCursorKey cursorKey);

    /**
     * 특정 사용자 기준 chat list row 1건 조회
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return chat list row DTO
     */
    Optional<ChatListItemResponse> findChatListItem(Long roomId, Long userId);

    List<ChatRoomListRow> findChatListItemsBulk(Long roomId, List<Long> userIds);
}
