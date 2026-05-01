package com.chat_server.redis.service;

import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.redis.dto.ChatRoomMetaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMetadataRedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ChatListRepository chatListRepository;

    // 방 전체의 마지막 메시지 정보 업데이트
    public void updateRoomMeta(Long roomId, Long messageId, String preview, LocalDateTime createdAt){
        String key = "chat:room:" + roomId + ":meta";
        Map<String, String> meta = Map.of(
                "lastMessageId", String.valueOf(messageId),
                "lastPreview", preview,
                "lastMessageAt", createdAt.toString()
        );
        redisTemplate.opsForHash().putAll(key, meta);
        redisTemplate.opsForSet().add("chat:room:dirty", roomId.toString());
    }

    // 개별 유저의 안읽음 카운트 증가
    public void incrementUnreadCount(Long roomId, Long userId){
        String key = "chat:room:" + roomId + ":user:" + userId + ":meta";

        // 올리기 전에 무조건 캐시에 값이 존재하도록 보장! (안 그러면 0부터 시작해서 덮어씌워짐)
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            getUnreadCount(roomId, userId);
        }
        redisTemplate.opsForHash().increment(key, "unreadCount", 1);
        redisTemplate.opsForSet().add("chat:user:dirty", roomId + ":" + userId);
    }

    // 읽을 때
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

    public int getUnreadCount(Long roomId, Long userId) {
        String key = "chat:room:" + roomId + ":user:" + userId + ":meta";
        Object cachedObj = redisTemplate.opsForHash().get(key, "unreadCount");

        if (cachedObj != null) {
            return Integer.parseInt(cachedObj.toString());
        }

        // Redis에 없으면 DB에서 조회해서 세팅 (Cache Warming)
        ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);
        int dbUnread = (chatList != null && chatList.getUnreadCount() != null) ? chatList.getUnreadCount() : 0;

        redisTemplate.opsForHash().put(key, "unreadCount", String.valueOf(dbUnread));
        return dbUnread;
    }

    public ChatRoomMetaDto getRoomMeta(Long roomId) {
        String key = "chat:room:" + roomId + ":meta";
        // redis map get
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) return null;

        return new ChatRoomMetaDto(
                Long.valueOf(entries.get("lastMessageId").toString()),
                entries.get("lastPreview").toString(),
                LocalDateTime.parse(entries.get("lastMessageAt").toString())
        );
    }


}
