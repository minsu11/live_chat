package com.chat_server.chatmessage.repository;

import com.chat_server.chatmessage.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.chatRoom.id = :roomId" +
            " AND m.id <= :messageId ORDER BY m.id DESC")
    List<ChatMessage> findRecentMessages(
            @Param("roomId") Long roomId,
            @Param("messageId") Long messageId,
            Pageable pageable
    );
}
