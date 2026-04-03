package com.chat_server.chatlist.service;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChatListService {
    /**
     * 커서 기반으로 채팅방 목록을 조회한다.
     *
     * @param userId 조회 사용자 ID
     * @param limit 페이지 크기
     * @param cursor 다음 페이지 조회를 위한 커서(없으면 첫 페이지)
     * @return 채팅방 목록 + next cursor + hasNext 정보를 담은 응답
     */
    CursorPageResponse<ChatRoomListResponse> getChatRoomListsByCursor(Long userId, int limit, @Nullable String cursor);

    /**
     * 채팅방-사용자 멤버십을 보장한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    void ensureMembership(Long roomId, Long userId);

    /**
     * 발신자를 제외한 멤버 unread count를 증가시킨다.
     *
     * @param roomId 채팅방 ID
     * @param senderId 발신자 ID
     */
    void increaseUnreadCount(Long roomId, Long senderId);

    /**
     * 채팅방에 속한 멤버 사용자 ID 목록을 조회한다.
     *
     * @param roomId 채팅방 ID
     * @return 멤버 사용자 ID 리스트
     */
    List<Long> getRoomMemberUserIds(Long roomId);

    /**
     * 사용자가 설정한 채팅방 커스텀 이름을 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 커스텀 이름(Optional)
     */
    Optional<String> getCustomRoomName(Long roomId, Long userId);

    /**
     * 채팅방 입장 시 최신 메시지 기준으로 읽음 상태를 반영한다.
     *
     * <p>처리 규칙:
     * <ul>
     *   <li>last_read_message_id는 더 작은 값으로 내려가지 않는다.</li>
     *   <li>unread_count는 0으로 초기화한다.</li>
     *   <li>last_opened_at을 현재 시각으로 갱신한다.</li>
     * </ul>
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param messageId 최신 메시지 ID
     * @return 업데이트된 row 수
     */
    int markAsReadOnEnter(Long roomId, Long userId, Long messageId);

    /**
     * 메시지가 없는 채팅방 입장 시 unread_count만 0으로 초기화한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 업데이트된 row 수
     */
    int clearUnreadCountOnEnter(Long roomId, Long userId);


    /**
     * 메시지 전송 시 발신자 본인의 chat_list를 읽은 상태로 맞춘다.
     *
     * <p>처리 규칙:
     * <ul>
     *   <li>last_read_message_id는 messageId보다 작을 때만 갱신한다.</li>
     *   <li>unread_count는 0으로 맞춘다.</li>
     *   <li>last_opened_at은 현재 시각으로 갱신한다.</li>
     * </ul>
     *
     * @param roomId 채팅방 ID
     * @param senderId 메시지 발신자 ID
     * @param messageId 저장된 메시지 ID
     * @return 업데이트된 row 수
     */
    int markSenderAsReadOnSend(Long roomId, Long senderId, Long messageId);

    /**
     * 실시간 읽음 처리 시 last_read_message_id와 unread_count를 갱신한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param messageId 읽은 메시지 ID
     * @return 업데이트된 row 수
     */
    int markAsRead(Long roomId, Long userId, Long messageId);

    /**
     * 특정 채팅방에서 여러 사용자의 unread count를 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param userIds 사용자 ID 목록
     * @return key=userId, value=unreadCount 맵
     */
    Map<Long, Integer> getUnreadCountMap(Long roomId, List<Long> userIds);
}
