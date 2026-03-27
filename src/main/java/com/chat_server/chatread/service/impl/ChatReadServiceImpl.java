package com.chat_server.chatread.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatread.dto.event.ChatReadUpdatedEvent;
import com.chat_server.chatread.dto.request.ChatReadRequest;
import com.chat_server.chatread.service.ChatReadService;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatReadServiceImpl implements ChatReadService {
    private final ChatRoomQueryService chatRoomQueryService;
    private final ChatListService chatListService;

    /**
     * 실시간 읽음 요청을 처리한다.
     *
     * <p>처리 순서:
     * <ol>
     *   <li>사용자가 해당 채팅방 멤버인지 검증한다.</li>
     *   <li>chat_list.last_read_message_id를 max 기준으로 갱신한다.</li>
     *   <li>chat_list.unread_count를 0으로 만든다.</li>
     * </ol>
     *
     * @param request 읽음 요청 DTO
     * @param userId 요청 사용자 ID
     * @return 읽음 갱신 이벤트 DTO
     */
    @Override
    public ChatReadUpdatedEvent read(ChatReadRequest request, Long userId) {
        Long roomId = request.roomId();
        Long messageId = request.messageId();

        chatRoomQueryService.validateMemberOrThrow(roomId, userId);

        int updatedRows = chatListService.markAsRead(roomId, userId, messageId);
        if (updatedRows == 0) {
            throw new IllegalStateException(
                "chat_list 멤버십이 없어 실시간 읽음 처리를 할 수 없습니다. roomId="
                    + roomId + ", userId=" + userId
            );
        }

        return new ChatReadUpdatedEvent(roomId, userId, messageId);
    }
}
