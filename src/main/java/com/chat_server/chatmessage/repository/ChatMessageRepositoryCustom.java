package com.chat_server.chatmessage.repository;

import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatread.dto.event.UpdatedMessageUnreadCount;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import org.springframework.data.domain.Slice;
import org.springframework.lang.Nullable;

import java.util.List;

public interface ChatMessageRepositoryCustom {

    /**
     * 채팅방 진입용 메시지를 커서 기반으로 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param limit 페이지 크기
     * @param cursorKey 커서 키(없으면 첫 페이지)
     * @return 메시지 Slice
     */
    Slice<ChatMessageItemResponse> getEnterMessagesByCursor(Long roomId,
                                                            Long userId,
                                                            int limit,
                                                            @Nullable ChatMessageCursorKey cursorKey);

    List<UpdatedMessageUnreadCount> findUpdatedUnreadCounts(Long roomId, Long lastReadMessageId);

    Slice<ChatMessageItemResponse> getMessagesAfter(Long roomId, Long afterMessageId, int limit);
}
