package com.chat_server.redis.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMetadataBatchScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    /**
     * jpa: @Modifying 벌크 연산은 동일한 값으로 일괄 업데이트할 때 좋지만,
     * 채팅방 메타 데이터처럼 각 row마다 갱신되는값이 다를 경우에는 결국 쿼리가 n번 발생
     * 네트워크 왕복 비용을 최소화하기 위해, 서로 다른 파라미터를 하나로 묶어 단 하나번의 네트워크 통신
     * 처리할 수 잇는 jdbc template batch update 전략으로 선택
     */
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void flushMetadataToDb() {
        log.info("Redis -> DB 메타 데이터 동기화 시작");
        flushUserMeta();
        flushRoomMeta();
    }

    private void flushUserMeta() {
        Set<String> dirtyUsers = redisTemplate.opsForSet().members("chat:user:dirty");
        if (dirtyUsers == null || dirtyUsers.isEmpty()) return;
        List<Object[]> batchArgs = new ArrayList<>();
        List<String> successKeys = new ArrayList<>();

        try {


            for (String member : dirtyUsers) {
                String[] parts = member.split(":");
                if (parts.length != 2) continue;

                long roomId = Long.parseLong(parts[0]);
                long userId = Long.parseLong(parts[1]);
                String key = "chat:room:" + roomId + ":user:" + userId + ":meta";

                Object unread = redisTemplate.opsForHash().get(key, "unreadCount");
                Object lastRead = redisTemplate.opsForHash().get(key, "lastReadMessageId");
                Object lastOpen = redisTemplate.opsForHash().get(key, "lastOpenedAt");

                if (unread != null) {
                    batchArgs.add(new Object[]{
                            Integer.parseInt(unread.toString()),
                            (lastRead != null && !lastRead.toString().equals("null")) ? Long.valueOf(lastRead.toString()) : null,
                            (lastOpen != null && !lastOpen.toString().equals("null")) ? LocalDateTime.parse(lastOpen.toString()) : LocalDateTime.now(),
                            roomId,
                            userId
                    });
                    successKeys.add(member);
                }

            }
            if (!batchArgs.isEmpty()) {
                String sql = "UPDATE chat_list SET unread_count = ?, last_read_message_id = ?, last_opened_at = ? WHERE chat_room_id = ? AND user_id = ?";
                jdbcTemplate.batchUpdate(sql, batchArgs);

                redisTemplate.opsForSet().remove("chat:user:dirty", successKeys.toArray());
                log.info("✅ 유저 메타데이터 {}건 Bulk Update 완료", batchArgs.size());
            }
        }catch (Exception e){
            // redis 복구
            log.error("❌ 유저 메타데이터 Bulk Update 실패 (다음 배치에서 재시도됨) - 사유: {}", e.getMessage());

        }
    }

    private void flushRoomMeta() {
        Set<String> dirtyRooms = redisTemplate.opsForSet().members("chat:room:dirty");
        if (dirtyRooms == null || dirtyRooms.isEmpty()) return;

        List<Object[]> batchArgs = new ArrayList<>();
        List<String> successKeys = new ArrayList<>();

        try {
            for (String roomIdStr : dirtyRooms) {
                Long roomId = Long.valueOf(roomIdStr);
                String key = "chat:room:" + roomId + ":meta";

                Map<Object, Object> meta = redisTemplate.opsForHash().entries(key);
                if (!meta.isEmpty() && meta.get("lastMessageId") != null) {
                    batchArgs.add(new Object[]{
                            Long.valueOf(meta.get("lastMessageId").toString()),
                            meta.get("lastMessagePreview") != null ? meta.get("lastMessagePreview").toString() : "",
                            meta.get("lastMessageAt") != null ? LocalDateTime.parse(meta.get("lastMessageAt").toString()) : LocalDateTime.now(),
                            roomId
                    });
                    successKeys.add(roomIdStr);
                }
            }

            // 🚀 채팅방 정보 단 한 번의 쿼리로 일괄 업데이트
            if (!batchArgs.isEmpty()) {
                String sql = "UPDATE chat_room SET last_message_id = ?, last_message_preview = ?, last_message_at = ? WHERE id = ?";
                jdbcTemplate.batchUpdate(sql, batchArgs);

                redisTemplate.opsForSet().remove("chat:room:dirty", successKeys.toArray());
                log.info("✅ 채팅방 메타데이터 {}건 Bulk Update 완료", batchArgs.size());
            }

        } catch (Exception e) {
            log.error("❌ 채팅방 메타데이터 Bulk Update 실패 (다음 배치에서 재시도됨) - 사유: {}", e.getMessage());
        }
    }
}