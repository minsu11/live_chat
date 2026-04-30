package com.chat_server.chatroom.repository;

import com.chat_server.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// chat room repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom,Long>, ChatRoomRepositoryCustom {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoom c SET c.participantCount = c.participantCount - 1 WHERE c.id = :roomId AND c.participantCount > 0")
    void decrementParticipantCount(@Param("roomId") Long roomId);

}
