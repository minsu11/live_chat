package com.chat_server.redis.service;

import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.redis.dto.ChatRoomMetaDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMetadataRedisService {

    private static final String SYNC_DB_MODE = "sync-db";

    private final RedisTemplate<String, String> redisTemplate;
    private final ChatListRepository chatListRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 성능 비교 브랜치에서만 사용하는 전환 값이다.
     *
     * <p>redis-write-back: Redis Hash/Dirty Set에 먼저 기록하고 Scheduler가 DB에 반영한다.
     * <p>sync-db: 현재 DB 스키마는 유지하면서 메타데이터를 요청 트랜잭션 안에서 즉시 갱신한다.
     */
    @Value("${chat.metadata.write-mode:redis-write-back}")
    private String writeMode;

    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "getLatestMessageIdFallback")
    public Long getLatestMessageId(Long roomId, Long entityLastId) {
        if (isSyncDbMode()) {
            return entityLastId;
        }

        String key = "chat:room:" + roomId + ":meta";
        Object redisId = redisTemplate.opsForHash().get(key, "lastMessageId");
        return redisId != null ? Long.valueOf(redisId.toString()) : entityLastId;
    }

    private Long getLatestMessageIdFallback(Long roomId, Long entityLastId, Throwable t) {
        return entityLastId;
    }

    // 방 전체의 마지막 메시지 정보 업데이트
    @Transactional
    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "updateRoomMetaFallback")
    public void updateRoomMeta(Long roomId, Long messageId, String preview, LocalDateTime createdAt) {
        if (isSyncDbMode()) {
            chatRoomRepository.syncRoomMetaFromRedis(roomId, messageId, preview, createdAt);
            return;
        }

        String key = "chat:room:" + roomId + ":meta";
        Map<String, String> meta = Map.of(
                "lastMessageId", String.valueOf(messageId),
                "lastMessagePreview", preview,
                "lastMessageAt", createdAt.toString()
        );
        redisTemplate.opsForHash().putAll(key, meta);
        redisTemplate.opsForSet().add("chat:room:dirty", roomId.toString());
    }

    @Transactional
    public void updateRoomMetaFallback(Long roomId, Long messageId, String preview, LocalDateTime createdAt, Throwable t) {
        log.warn("[CircuitBreaker] Redis 장애로 채팅방({}) 메타데이터를 DB에 직접 저장합니다. 사유: {}", roomId, t.getMessage());
        chatRoomRepository.syncRoomMetaFromRedis(roomId, messageId, preview, createdAt);
    }

    // 개별 유저의 안읽음 카운트 증가
    @Transactional
    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "incrementUnreadCountFallback")
    public void incrementUnreadCount(Long roomId, Long userId) {
        if (isSyncDbMode()) {
            // 현재 메서드는 수신자별로 호출되므로 특정 사용자 row만 1 증가시킨다.
            chatListRepository.addUnreadCountBatch(roomId, userId, 1);
            return;
        }

        String key = "chat:room:" + roomId + ":user:" + userId + ":meta";

        // 증가 전에 캐시에 값이 존재하도록 보장한다. 없으면 DB 값으로 워밍한다.
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            getUnreadCount(roomId, userId);
        }

        redisTemplate.opsForHash().increment(key, "unreadCount", 1);
        redisTemplate.opsForSet().add("chat:user:dirty", roomId + ":" + userId);
    }

    @Transactional
    public void incrementUnreadCountFallback(Long roomId, Long userId, Throwable t) {
        log.warn("[CircuitBreaker] Redis 장애로 유저({}, 방:{}) 안읽음 카운트를 DB에 직접 증가시킵니다.", userId, roomId);
        chatListRepository.addUnreadCountBatch(roomId, userId, 1);
    }

    // 읽을 때
    @Transactional
    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "markAsReadFallback")
    public void markAsRead(Long roomId, Long userId, Long messageId, LocalDateTime openedAt) {
        if (isSyncDbMode()) {
            chatListRepository.syncUserMetaFromRedis(roomId, userId, 0, messageId, openedAt);
            return;
        }

        String key = "chat:room:" + roomId + ":user:" + userId + ":meta";

        Map<String, String> meta = Map.of(
                "lastReadMessageId", String.valueOf(messageId),
                "unreadCount", "0",
                "lastOpenedAt", openedAt.toString()
        );

        redisTemplate.opsForHash().putAll(key, meta);
        redisTemplate.opsForSet().add("chat:user:dirty", roomId + ":" + userId);
    }

    @Transactional
    public void markAsReadFallback(Long roomId, Long userId, Long messageId, LocalDateTime openedAt, Throwable t) {
        log.warn("[CircuitBreaker] Redis 장애로 유저({}, 방:{}) 읽음 처리를 DB에 직접 반영합니다.", userId, roomId);
        chatListRepository.syncUserMetaFromRedis(roomId, userId, 0, messageId, openedAt);
    }

    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "getUnreadCountFallback")
    public int getUnreadCount(Long roomId, Long userId) {
        if (isSyncDbMode()) {
            return getUnreadCountFromDb(roomId, userId);
        }

        String key = "chat:room:" + roomId + ":user:" + userId + ":meta";
        Object cachedObj = redisTemplate.opsForHash().get(key, "unreadCount");
        if (cachedObj != null) {
            return Integer.parseInt(cachedObj.toString());
        }

        int dbUnread = getUnreadCountFromDb(roomId, userId);

        // 캐시 워밍
        redisTemplate.opsForHash().put(key, "unreadCount", String.valueOf(dbUnread));
        return dbUnread;
    }

    private int getUnreadCountFallback(Long roomId, Long userId, Throwable t) {
        log.warn("[CircuitBreaker] Redis 조회 실패로 유저({}, 방:{}) 안읽음 카운트를 DB에서 조회합니다.", userId, roomId);
        return getUnreadCountFromDb(roomId, userId);
    }

    public ChatRoomMetaDto getRoomMeta(Long roomId) {
        if (isSyncDbMode()) {
            ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
            if (room == null || room.getLastMessageId() == null) {
                return null;
            }
            return new ChatRoomMetaDto(
                    room.getLastMessageId(),
                    room.getLastMessagePreview(),
                    room.getLastMessageAt()
            );
        }

        try {
            String key = "chat:room:" + roomId + ":meta";
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries.isEmpty()) return null;

            return new ChatRoomMetaDto(
                    Long.valueOf(entries.get("lastMessageId").toString()),
                    entries.get("lastMessagePreview").toString(),
                    LocalDateTime.parse(entries.get("lastMessageAt").toString())
            );
        } catch (Exception e) {
            return null;
        }
    }

    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "getAllMembersLastReadIdFallback")
    public Map<Long, Long> getAllMembersLastReadId(Long roomId, List<Long> memberIds) {
        if (isSyncDbMode()) {
            return getAllMembersLastReadIdFromDb(roomId, memberIds);
        }

        Map<Long, Long> memberReadMap = new HashMap<>();
        for (Long userId : memberIds) {
            String key = "chat:room:" + roomId + ":user:" + userId + ":meta";
            Object lastReadIdObj = redisTemplate.opsForHash().get(key, "lastReadMessageId");

            if (lastReadIdObj != null) {
                memberReadMap.put(userId, Long.valueOf(lastReadIdObj.toString()));
            } else {
                ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);
                Long dbLastReadId = (chatList != null && chatList.getLastReadMessageId() != null)
                        ? chatList.getLastReadMessageId()
                        : 0L;
                memberReadMap.put(userId, dbLastReadId);

                // 캐시 워밍
                redisTemplate.opsForHash().put(key, "lastReadMessageId", String.valueOf(dbLastReadId));
            }
        }
        return memberReadMap;
    }

    public Map<Long, Long> getAllMembersLastReadIdFallback(Long roomId, List<Long> memberIds, Throwable t) {
        log.warn("[CircuitBreaker] Redis 조회 실패로 채팅방({}) 멤버 전체 읽음 상태를 DB에서 조회합니다.", roomId);
        return getAllMembersLastReadIdFromDb(roomId, memberIds);
    }

    private int getUnreadCountFromDb(Long roomId, Long userId) {
        ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);
        return (chatList != null && chatList.getUnreadCount() != null) ? chatList.getUnreadCount() : 0;
    }

    private Map<Long, Long> getAllMembersLastReadIdFromDb(Long roomId, List<Long> memberIds) {
        Map<Long, Long> memberReadMap = new HashMap<>();
        for (Long userId : memberIds) {
            ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);
            Long dbLastReadId = (chatList != null && chatList.getLastReadMessageId() != null)
                    ? chatList.getLastReadMessageId()
                    : 0L;
            memberReadMap.put(userId, dbLastReadId);
        }
        return memberReadMap;
    }

    private boolean isSyncDbMode() {
        return SYNC_DB_MODE.equalsIgnoreCase(writeMode);
    }

    @SuppressWarnings("unused")
    private void initUserMeta(Long roomId, Long userId) {
        try {
            String key = "chat:room:" + roomId + ":user:" + userId + ":meta";
            ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);

            String dbCount = (chatList != null && chatList.getUnreadCount() != null)
                    ? String.valueOf(chatList.getUnreadCount())
                    : "0";
            String dbLastMsgId = (chatList != null && chatList.getLastReadMessageId() != null)
                    ? String.valueOf(chatList.getLastReadMessageId())
                    : "0";

            Map<String, String> initialMeta = Map.of(
                    "unreadCount", dbCount,
                    "lastReadMessageId", dbLastMsgId,
                    "lastOpenedAt", LocalDateTime.now().toString()
            );
            redisTemplate.opsForHash().putAll(key, initialMeta);
        } catch (Exception ignored) {
        }
    }
}
