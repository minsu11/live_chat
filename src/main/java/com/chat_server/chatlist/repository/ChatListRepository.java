package com.chat_server.chatlist.repository;

import com.chat_server.chatlist.dto.response.ChatUnreadCountRow;
import com.chat_server.chatlist.entity.ChatList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /**
     * 사용자가 특정 채팅방에 설정한 커스텀 방 이름을 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 커스텀 방 이름(Optional)
     */
    @Query("""
        select cl.customName
        from ChatList cl
        where cl.chatRoom.id = :roomId
          and cl.user.id = :userId
          and cl.customName is not null
          and cl.customName <> ''
        """)
    Optional<String> findCustomNameByUserIdAndRoomId(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );

    /**
     * 채팅방 입장 시 읽음 상태를 갱신한다.
     *
     * <p>규칙:
     * <ul>
     *   <li>last_read_message_id는 현재 값보다 큰 경우에만 갱신한다.</li>
     *   <li>unread_count는 항상 0으로 만든다.</li>
     *   <li>last_opened_at은 현재 시각으로 갱신한다.</li>
     * </ul>
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param messageId 최신 메시지 ID
     * @param openedAt 입장 시각
     * @return 업데이트된 row 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ChatList cl
           set cl.lastReadMessageId =
                case
                    when cl.lastReadMessageId is null then :messageId
                    when cl.lastReadMessageId < :messageId then :messageId
                    else cl.lastReadMessageId
                end,
               cl.unreadCount = 0,
               cl.lastOpenedAt = :openedAt
         where cl.chatRoom.id = :roomId
           and cl.user.id = :userId
    """)
    int markAsReadOnEnter(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("messageId") Long messageId,
            @Param("openedAt") LocalDateTime openedAt
    );

    /**
     * 메시지가 없는 채팅방 입장 시 unread_count만 0으로 초기화한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param openedAt 입장 시각
     * @return 업데이트된 row 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ChatList cl
           set cl.unreadCount = 0,
               cl.lastOpenedAt = :openedAt
         where cl.chatRoom.id = :roomId
           and cl.user.id = :userId
    """)
    int clearUnreadCountOnEnter(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("openedAt") LocalDateTime openedAt
    );

    /**
     * 메시지 전송 시 발신자 본인의 chat_list를 읽은 상태로 맞춘다.
     *
     * @param roomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param messageId 저장된 메시지 ID
     * @param openedAt 처리 시각
     * @return 업데이트된 row 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ChatList cl
           set cl.lastReadMessageId =
                case
                    when cl.lastReadMessageId is null then :messageId
                    when cl.lastReadMessageId < :messageId then :messageId
                    else cl.lastReadMessageId
                end,
               cl.unreadCount = 0,
               cl.lastOpenedAt = :openedAt
         where cl.chatRoom.id = :roomId
           and cl.user.id = :senderId
    """)
    int markSenderAsReadOnSend(
            @Param("roomId") Long roomId,
            @Param("senderId") Long senderId,
            @Param("messageId") Long messageId,
            @Param("openedAt") LocalDateTime openedAt
    );

    /**
     * 특정 채팅방에서 여러 사용자의 unread count를 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param userIds 사용자 ID 목록
     * @return 사용자별 unread count 조회 결과
     */
    @Query("""
        select new com.chat_server.chatlist.dto.response.ChatUnreadCountRow(
            cl.user.id,
            cl.unreadCount
        )
        from ChatList cl
        where cl.chatRoom.id = :roomId
          and cl.user.id in :userIds
    """)
    List<ChatUnreadCountRow> findUnreadCountRows(
            @Param("roomId") Long roomId,
            @Param("userIds") List<Long> userIds
    );
}
