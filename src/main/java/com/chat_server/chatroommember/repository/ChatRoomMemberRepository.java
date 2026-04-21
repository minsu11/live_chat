package com.chat_server.chatroommember.repository;

import com.chat_server.chatroommember.entity.ChatRoomMember;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long>, ChatRoomMemberRepositoryCustom {
    @Modifying
    @Query(value = """
        INSERT INTO chat_room_member (chat_room_id, user_id, role, joined_at, is_active)
        VALUES (?1, ?2, ?3, NOW(), 1)
        ON DUPLICATE KEY UPDATE
            is_active = 1,
            left_at = NULL
        """, nativeQuery = true)
    void upsertMembership(long roomId, long userId, String role);

    @Query("""
        select case when count(m) > 0 then true else false end
        from ChatRoomMember m
        where m.chatRoom.id = :roomId
          and m.user.id = :userId
          and m.active = true
    """)
    boolean existsActiveMember(@Param("roomId") Long roomId, @Param("userId") Long userId);

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

}
