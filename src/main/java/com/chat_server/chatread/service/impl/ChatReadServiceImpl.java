package com.chat_server.chatread.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatread.service.ChatReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatReadServiceImpl implements ChatReadService {
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
     * @param roomId 요청 방 ID
     * @param userId 요청 사용자 ID
     * @param messageId 요청 메세지 아이디
     */
    @Override
    public void markAsRead(Long roomId, Long userId, Long messageId) {

        int updatedRows = chatListService.markAsRead(roomId, userId, messageId);
        if (updatedRows == 0) {
            throw new IllegalStateException(
                "chat_list 멤버십이 없어 실시간 읽음 처리를 할 수 없습니다. roomId="
                    + roomId + ", userId=" + userId
            );
        }

    }

    /**
     * 채팅방 입장 시 unread_count를 0으로 만들고,
     * 최신 메시지가 있으면 last_read_message_id를 해당 값까지 올린다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param latestMessageId 최신 메시지 ID. null이면 메시지가 없는 방으로 간주한다.
     */
    @Override
    public void markAsReadOnEnter(Long roomId, Long userId, Long latestMessageId) {
        Long safeMessageId = (latestMessageId != null) ? latestMessageId : 0L;

        int updateRows = chatListService.markAsRead(roomId,userId,safeMessageId);

        if(updateRows == 0){
            throw new IllegalArgumentException("멤버쉽 없음");
        }
    }
}
