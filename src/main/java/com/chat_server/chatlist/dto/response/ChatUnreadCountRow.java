package com.chat_server.chatlist.dto.response;

/**
 * 채팅방 내 사용자별 unread count 조회 결과를 담는다.
 *
 * @param userId 사용자 ID
 * @param unreadCount unread count
 */
public record ChatUnreadCountRow(
        Long userId,
        Integer unreadCount
) {
}