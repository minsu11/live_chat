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
    /**
     * 🎯 Full-Text Search (N-gram) 기반 메시지 검색
     * BOOLEAN MODE를 사용하여 검색어의 부분 일치(+키워드*)를 지원합니다.
     */
    @Query(value =
            "SELECT * FROM chat_message " +
                    "WHERE chat_room_id = :roomId " +
                    "AND MATCH(message_content) AGAINST(CONCAT('+', :keyword, '*') IN BOOLEAN MODE) " +
                    "ORDER BY created_at DESC " +
                    "LIMIT :limit",
            nativeQuery = true)
    List<ChatMessage> searchMessagesByKeyword(
            @Param("roomId") Long roomId,
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    // 1. 타겟 포함 과거 메시지 조회 (내림차순 조회 후 서비스에서 뒤집음)
    @Query(value = "SELECT * FROM chat_message " +
            "WHERE chat_room_id = :roomId " +
            "AND id <= :targetId " +
            "ORDER BY id DESC LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findOlderMessagesWithTarget(@Param("roomId") Long roomId, @Param("targetId") Long targetId, @Param("limit") int limit);

    // 2. 미래 메시지 조회 (오름차순)
    @Query(value = "SELECT * FROM chat_message" +
            " WHERE chat_room_id = :roomId " +
            "AND id > :targetId " +
            "ORDER BY id ASC LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findNewerMessages(@Param("roomId") Long roomId, @Param("targetId") Long targetId, @Param("limit") int limit);

}
