package com.chat_server.chatlist.repository;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.common.cursor.ChatListCursorKey;
import com.chat_server.common.cursor.CursorKey;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.NoRepositoryBean;

public interface ChatListRepositoryCustom {
    Slice<ChatRoomListResponse> getChatRoomListByCursor(Long userId, int limit, ChatListCursorKey cursorKey);
}
