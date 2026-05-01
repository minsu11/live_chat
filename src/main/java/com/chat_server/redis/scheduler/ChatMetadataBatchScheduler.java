package com.chat_server.redis.scheduler;

import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMetadataBatchScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final ChatListRepository chatListRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void flushMetadataToDb() {
        flushUserMeta();
        flushRoomMeta();
    }

    private void flushUserMeta() {
        Set<String> dirtyUsers = redisTemplate.opsForSet().members("chat:user:dirty");
        if (dirtyUsers == null || dirtyUsers.isEmpty()) return;

        for (String member : dirtyUsers) {
            String[] parts = member.split(":");
            Long roomId = Long.valueOf(parts[0]);
            Long userId = Long.valueOf(parts[1]);
            String key = "chat:room:" + roomId + ":user:" + userId + ":meta";

            Object unread = redisTemplate.opsForHash().get(key, "unreadCount");
            Object lastRead = redisTemplate.opsForHash().get(key, "lastReadMessageId");
            Object lastOpen = redisTemplate.opsForHash().get(key, "lastOpenedAt");

            if (unread != null) {
                // ChatListRepository에 syncUserMetaFromRedis 메서드가 있어야 함 (앞선 답변 참고)
                chatListRepository.syncUserMetaFromRedis(
                        roomId, userId,
                        Integer.parseInt(unread.toString()),
                        lastRead != null ? Long.valueOf(lastRead.toString()) : null,
                        lastOpen != null ? LocalDateTime.parse(lastOpen.toString()) : LocalDateTime.now()
                );
            }
            redisTemplate.opsForSet().remove("chat:user:dirty", member);
        }
    }

    private void flushRoomMeta() {
        Set<String> dirtyRooms = redisTemplate.opsForSet().members("chat:room:dirty");
        if (dirtyRooms == null || dirtyRooms.isEmpty()) return;

        for (String roomIdStr : dirtyRooms) {
            Long roomId = Long.valueOf(roomIdStr);
            String key = "chat:room:" + roomId + ":meta";

            Map<Object, Object> meta = redisTemplate.opsForHash().entries(key);
            if (!meta.isEmpty()) {
                chatRoomRepository.syncRoomMetaFromRedis(
                        roomId,
                        Long.valueOf(meta.get("lastMessageId").toString()),
                        meta.get("lastPreview").toString(),
                        LocalDateTime.parse(meta.get("lastMessageAt").toString())
                );
            }
            redisTemplate.opsForSet().remove("chat:room:dirty", roomIdStr);
        }
    }
}