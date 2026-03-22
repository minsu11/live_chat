package com.chat_server.chatlist.repository;

import com.chat_server.chatlist.entity.ChatList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatListRepository extends JpaRepository<ChatList, Long>,ChatListRepositoryCustom {
    /**
     * 채팅방-사용자 멤버십을 upsert 한다.
     *
     * <p>동작:
     * <ul>
     *   <li>신규 관계면 row를 생성한다.</li>
     *   <li>이미 존재하면 no-op(자기 자신 업데이트)으로 중복 삽입을 방지한다.</li>
     * </ul>
     *
     * @param roomId 대상 채팅방 ID
     * @param userId 멤버십을 보장할 사용자 ID
     */
    @Modifying
    @Query(value = """
    INSERT INTO chat_list (chat_room_id, user_id,unread_count, pinned, muted, archived)
    VALUES (?1, ?2,0, 0, 0, 0)
    ON DUPLICATE KEY UPDATE user_id = user_id
    """, nativeQuery = true)
    void upsertMembership(long roomId, long userId);

    /**
     * 특정 채팅방에서 발신자를 제외한 멤버의 unread_count를 1 증가시킨다.
     *
     * @param roomId unread 증가 대상 채팅방 ID
     * @param senderId 현재 메시지를 보낸 발신자 ID(증가 대상에서 제외)
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
    UPDATE chat_list
    SET unread_count = unread_count + 1
    WHERE chat_room_id = :roomId
      AND user_id <> :senderId
""", nativeQuery = true)
    void increaseUnreadCount(
        long roomId,
        long senderId
    );

    /**
     * 채팅방에 속한 사용자 ID 목록을 조회한다.
     *
     * @param roomId 조회 대상 채팅방 ID
     * @return 채팅방 멤버 사용자 ID 리스트(없으면 빈 리스트)
     */
    @Query(value = """
        SELECT user_id
        FROM chat_list
        WHERE chat_room_id = :roomId
        """, nativeQuery = true)
    List<Long> findUserIdsByRoomId(@Param("roomId") long roomId);
}
