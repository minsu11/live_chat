package com.chat_server.redis.service;

import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.redis.dto.ChatRoomMetaDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RedisTemplate<String, String> redisTemplate;
    private final ChatListRepository chatListRepository;
    private final ChatRoomRepository chatRoomRepository;

    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "getLatestMessageIdFallback")
    public Long getLatestMessageId(Long roomId, Long entityLastId) {
        String key = "chat:room:" + roomId + ":meta";
        Object redisId = redisTemplate.opsForHash().get(key, "lastMessageId");
        return redisId != null ? Long.valueOf(redisId.toString()) : entityLastId;
    }

    private Long getLatestMessageIdFallback(Long roomId, Long entityLastId, Throwable t) {
        return entityLastId;
    }


    // 방 전체의 마지막 메시지 정보 업데이트
    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "updateRoomMetaFallback")
    public void updateRoomMeta(Long roomId, Long messageId, String preview, LocalDateTime createdAt){
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
        log.warn("🚨 [CircuitBreaker] Redis 장애! 채팅방({}) 메타데이터를 DB에 직접 저장합니다. 사유: {}", roomId, t.getMessage());
        chatRoomRepository.syncRoomMetaFromRedis(roomId, messageId, preview, createdAt);
    }

    // 개별 유저의 안읽음 카운트 증가
    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "incrementUnreadCountFallback")
    public void incrementUnreadCount(Long roomId, Long userId){
        String key = "chat:room:" + roomId + ":user:" + userId + ":meta";

        // 올리기 전에 무조건 캐시에 값이 존재하도록 보장! (안 그러면 0부터 시작해서 덮어씌워짐)
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            getUnreadCount(roomId, userId);
        }

        redisTemplate.opsForHash().increment(key, "unreadCount", 1);
        redisTemplate.opsForSet().add("chat:user:dirty", roomId + ":" + userId);
    }

    @Transactional
    public void incrementUnreadCountFallback(Long roomId, Long userId, Throwable t) {
        log.warn("🚨 [CircuitBreaker] Redis 장애! 유저({}, 방:{}) 안읽음 카운트를 DB에 직접 증가시킵니다.", userId, roomId);
        // DB의 unread_count를 직접 +1 하는 Repository 쿼리 호출 (또는 Service 호출)
        chatListRepository.increaseUnreadCount(roomId, userId);
    }

    // 읽을 때
    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "markAsReadFallback")
    public void markAsRead(Long roomId, Long userId, Long messageId, LocalDateTime openedAt) {
        String key = "chat:room:" + roomId + ":user:" + userId + ":meta";

        Map<String, String> meta = Map.of(
                "lastReadMessageId", String.valueOf(messageId),
                "unreadCount", "0", // 🎯 무조건 0으로 덮어씀 (Delta 누적 버그 해결)
                "lastOpenedAt", openedAt.toString()
        );

        redisTemplate.opsForHash().putAll(key, meta);
        redisTemplate.opsForSet().add("chat:user:dirty", roomId + ":" + userId);
    }

    @Transactional
    public void markAsReadFallback(Long roomId, Long userId, Long messageId, LocalDateTime openedAt, Throwable t) {
        log.warn("🚨 [CircuitBreaker] Redis 장애! 유저({}, 방:{}) 읽음 처리를 DB에 직접 반영합니다.", userId, roomId);
        chatListRepository.syncUserMetaFromRedis(roomId, userId, 0, messageId, openedAt);
    }


    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "getUnreadCountFallback")
    public int getUnreadCount(Long roomId, Long userId) {
        String key = "chat:room:" + roomId + ":user:" + userId + ":meta";
        Object cachedObj = redisTemplate.opsForHash().get(key, "unreadCount");
        if (cachedObj != null) {
            return Integer.parseInt(cachedObj.toString());
        }

        ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);
        int dbUnread = (chatList != null && chatList.getUnreadCount() != null) ? chatList.getUnreadCount() : 0;

        // 캐시 워밍
        redisTemplate.opsForHash().put(key, "unreadCount", String.valueOf(dbUnread));
        return dbUnread;
    }

    private int getUnreadCountFallback(Long roomId, Long userId, Throwable t) {
        log.warn("🚨 [CircuitBreaker] Redis 대기 생략! 유저({}, 방:{}) 안읽음 카운트 DB 고속 조회", userId, roomId);
        ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);
        return (chatList != null && chatList.getUnreadCount() != null) ? chatList.getUnreadCount() : 0;
    }


    public ChatRoomMetaDto getRoomMeta(Long roomId) {
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
        Map<Long, Long> memberReadMap = new HashMap<>();
        for (Long userId : memberIds) {
            String key = "chat:room:" + roomId + ":user:" + userId + ":meta";
            Object lastReadIdObj = redisTemplate.opsForHash().get(key, "lastReadMessageId");

            if (lastReadIdObj != null) {
                log.info("if문 안으로 들어옴");
                memberReadMap.put(userId, Long.valueOf(lastReadIdObj.toString()));
            } else {
                log.info("else문");
                ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);
                Long dbLastReadId = (chatList != null && chatList.getLastReadMessageId() != null) ? chatList.getLastReadMessageId() : 0L;
                memberReadMap.put(userId, dbLastReadId);

                // 캐시 워밍
                redisTemplate.opsForHash().put(key, "lastReadMessageId", String.valueOf(dbLastReadId));
            }
        }
        return memberReadMap;
    }

    /**
     * 🚨 Redis 장애 시 (차단기 OPEN 상태):
     * 1초도 기다리지 않고, Redis 캐시 워밍(put) 시도조차 하지 않으며, 오직 순수하게 DB만 빠르게 조회해서 리턴합니다.
     */
    public Map<Long, Long> getAllMembersLastReadIdFallback(Long roomId, List<Long> memberIds, Throwable t) {
        log.warn("🚨 [CircuitBreaker] Redis 대기 생략! 채팅방({}) 멤버 전체 읽음 상태를 DB에서 고속 직접 조회합니다.", roomId);
        Map<Long, Long> memberReadMap = new HashMap<>();
        for (Long userId : memberIds) {
            ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);
            Long dbLastReadId = (chatList != null && chatList.getLastReadMessageId() != null) ? chatList.getLastReadMessageId() : 0L;
            memberReadMap.put(userId, dbLastReadId);
        }
        return memberReadMap;
    }


    private void initUserMeta(Long roomId, Long userId) {
        try {
            String key = "chat:room:" + roomId + ":user:" + userId + ":meta";
            ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);

            String dbCount = (chatList != null && chatList.getUnreadCount() != null) ? String.valueOf(chatList.getUnreadCount()) : "0";
            String dbLastMsgId = (chatList != null && chatList.getLastReadMessageId() != null) ? String.valueOf(chatList.getLastReadMessageId()) : "0";

            Map<String, String> initialMeta = Map.of(
                    "unreadCount", dbCount,
                    "lastReadMessageId", dbLastMsgId,
                    "lastOpenedAt", LocalDateTime.now().toString()
            );
            redisTemplate.opsForHash().putAll(key, initialMeta);
        } catch (Exception ignored) { }
    }


}
