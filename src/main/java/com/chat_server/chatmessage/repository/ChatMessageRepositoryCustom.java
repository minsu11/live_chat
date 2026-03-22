package com.chat_server.chatmessage.repository;

import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import org.springframework.data.domain.Slice;
import org.springframework.lang.Nullable;

public interface ChatMessageRepositoryCustom {
    Slice<ChatMessageItemResponse> getEnterMessagesByCursor(Long roomId, int limit,
                                                            @Nullable ChatMessageCursorKey cursorKey);
}
