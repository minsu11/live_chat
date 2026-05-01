package com.chat_server.redis.service;

import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.redis.dto.ChatRoomMetaDto;
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

    public Long getLatestMessageId(Long roomId, Long entityLastId) {
        String key = "chat:room:" + roomId + ":meta";
        Object redisId = redisTemplate.opsForHash().get(key, "lastMessageId");
        return redisId != null ? Long.valueOf(redisId.toString()) : entityLastId;
    }



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

        ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);
        int dbUnread = (chatList != null && chatList.getUnreadCount() != null) ? chatList.getUnreadCount() : 0;

        redisTemplate.opsForHash().put(key, "unreadCount", String.valueOf(dbUnread));
        return dbUnread;
    }

    public ChatRoomMetaDto getRoomMeta(Long roomId) {
        String key = "chat:room:" + roomId + ":meta";
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) return null;

        return new ChatRoomMetaDto(
                Long.valueOf(entries.get("lastMessageId").toString()),
                entries.get("lastPreview").toString(),
                LocalDateTime.parse(entries.get("lastMessageAt").toString())
        );
    }

    public Map<Long, Long> getAllMembersLastReadId(Long roomId, List<Long> memberIds) {
        Map<Long, Long> memberReadMap = new HashMap<>();
        for (Long userId : memberIds) {
            String key = "chat:room:" + roomId + ":user:" + userId + ":meta";
            Object lastReadIdObj = redisTemplate.opsForHash().get(key, "lastReadMessageId");

            if (lastReadIdObj != null) {
                memberReadMap.put(userId, Long.valueOf(lastReadIdObj.toString()));
            } else {
                // Redis에 없으면 DB에서 가져와서 채워넣음 (Warming)
                ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);
                Long dbLastReadId = (chatList != null && chatList.getLastReadMessageId() != null)
                        ? chatList.getLastReadMessageId() : 0L;
                memberReadMap.put(userId, dbLastReadId);
                // 캐시 보정
                redisTemplate.opsForHash().put(key, "lastReadMessageId", String.valueOf(dbLastReadId));
            }
        }
        return memberReadMap;
    }


    private void initUserMeta(Long roomId, Long userId) {
        String key = "chat:room:" + roomId + ":user:" + userId + ":meta";
        ChatList chatList = chatListRepository.findByChatRoomIdAndUserId(roomId, userId).orElse(null);

        // DB 값이 null이거나 unreadCount가 null인 경우 0으로 세팅
        String dbCount = (chatList != null && chatList.getUnreadCount() != null)
                ? String.valueOf(chatList.getUnreadCount()) : "0";
        String dbLastMsgId = (chatList != null && chatList.getLastReadMessageId() != null)
                ? String.valueOf(chatList.getLastReadMessageId()) : "0";

        Map<String, String> initialMeta = Map.of(
                "unreadCount", dbCount,
                "lastReadMessageId", dbLastMsgId,
                "lastOpenedAt", LocalDateTime.now().toString()
        );
        // putAll을 사용하여 원자적으로 초기값 세팅
        redisTemplate.opsForHash().putAll(key, initialMeta);
    }


}
