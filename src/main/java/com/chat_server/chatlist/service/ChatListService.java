package com.chat_server.chatlist.service;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import jakarta.annotation.Nullable;

import java.util.List;
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
}
