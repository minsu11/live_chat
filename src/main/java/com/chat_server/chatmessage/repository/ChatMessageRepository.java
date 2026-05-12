package com.chat_server.chatmessage.repository;

import com.chat_server.chatmessage.entity.ChatMessage;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
    @Query(
        value = """
        select m.*
        from chat_message m
        where m.chat_room_id = :roomId
          and m.message_content is not null
          and m.message_type in ('TEXT', 'EMOJI')
          and (
                match(m.message_content) against(:booleanKeyword in boolean mode)
                or m.message_content like concat('%', :rawKeyword, '%')
          )
          and (
                :cursorCreatedAt is null
                or :cursorMessageId is null
                or m.created_at < :cursorCreatedAt
                or (
                    m.created_at = :cursorCreatedAt
                    and m.id < :cursorMessageId
                )
          )
        order by m.created_at desc, m.id desc
        limit :limit
    """,
        nativeQuery = true
    )
    List<ChatMessage> searchMessagesByKeyword(
        @Param("roomId") Long roomId,
        @Param("rawKeyword") String rawKeyword,
        @Param("booleanKeyword") String booleanKeyword,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorMessageId") Long cursorMessageId,
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


    Optional<ChatMessage>  findByIdAndChatRoom_Id(Long messageId, Long roomId);
}
