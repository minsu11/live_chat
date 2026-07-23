-- 실행 전에 아래 값을 실제 테스트 값으로 변경한다.
SET @room_id = 1;
SET @run_prefix = 'sync-db-20260720-120000';

-- 1. 해당 실행에서 DB에 저장된 메시지 수
SELECT COUNT(*) AS stored_message_count
FROM chat_message
WHERE chat_room_id = @room_id
  AND client_message_id LIKE CONCAT(@run_prefix, '%');

-- 2. 중복 clientMessageId 여부. 결과가 없어야 한다.
SELECT client_message_id, COUNT(*) AS duplicate_count
FROM chat_message
WHERE chat_room_id = @room_id
  AND client_message_id LIKE CONCAT(@run_prefix, '%')
GROUP BY client_message_id
HAVING COUNT(*) > 1;

-- 3. 채팅방 마지막 메시지 메타데이터와 실제 최신 메시지 비교
SELECT
    cr.id AS room_id,
    cr.last_message_id AS room_last_message_id,
    latest.id AS actual_latest_message_id,
    cr.last_message_at AS room_last_message_at,
    latest.created_at AS actual_latest_message_at,
    CASE WHEN cr.last_message_id = latest.id THEN 'MATCH' ELSE 'MISMATCH' END AS result
FROM chat_room cr
JOIN (
    SELECT cm.id, cm.chat_room_id, cm.created_at
    FROM chat_message cm
    JOIN (
        SELECT chat_room_id, MAX(id) AS max_id
        FROM chat_message
        WHERE chat_room_id = @room_id
        GROUP BY chat_room_id
    ) max_message ON max_message.max_id = cm.id
) latest ON latest.chat_room_id = cr.id
WHERE cr.id = @room_id;

-- 4. 사용자별 채팅 목록 메타데이터 확인
SELECT
    chat_room_id,
    user_id,
    unread_count,
    last_read_message_id,
    last_opened_at
FROM chat_list
WHERE chat_room_id = @room_id
ORDER BY user_id;
