package com.chat_server.chatlist.repository;

import com.chat_server.chatlist.entity.ChatList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChatListRepository extends JpaRepository<ChatList, Long>,ChatListRepositoryCustom {
    @Modifying
    @Query(value = """
    INSERT INTO chat_list (chat_room_id, user_id,unread_count, pinned, muted, archived)
    VALUES (?1, ?2,0, 0, 0, 0)
    ON DUPLICATE KEY UPDATE user_id = user_id
    """, nativeQuery = true)
    void upsertMembership(long roomId, long userId);

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
}
