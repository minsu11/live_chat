package com.chat_server.chatroom.repository;

import com.chat_server.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

// chat room repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom,Long>, ChatRoomRepositoryCustom {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoom c SET c.participantCount = c.participantCount - 1 WHERE c.id = :roomId AND c.participantCount > 0")
    void decrementParticipantCount(@Param("roomId") Long roomId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
    UPDATE chat_room
    SET last_message_id = :lastMsgId,
        last_message_preview = :preview,
        last_message_at = :lastMsgAt
    WHERE id = :roomId
""", nativeQuery = true)
    void syncRoomMetaFromRedis(
            @Param("roomId") Long roomId,
            @Param("lastMsgId") Long lastMsgId,
            @Param("preview") String preview,
            @Param("lastMsgAt") LocalDateTime lastMsgAt
    );
}
