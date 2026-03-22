package com.chat_server.common.cursor;

import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ChatMessageCursorCodec {

    /**
     * 커서 키(메시지 시간/메시지 ID)를 Base64 URL-safe 문자열로 인코딩한다.
     *
     * @param lastMessageAtEpochMillis 마지막 메시지의 UTC epoch millis
     * @param lastMessageId 마지막 메시지 ID
     * @return API에서 사용할 커서 문자열
     */
    public static String encode(long lastMessageAtEpochMillis, long lastMessageId) {
        String raw = lastMessageAtEpochMillis + ":" + lastMessageId;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 커서를 디코딩하여 커서 키로 변환한다.
     *
     * @param cursor API로부터 전달받은 커서 문자열(Nullable)
     * @return 디코딩된 커서 키. 커서가 비어있거나 형식이 깨진 경우 null
     *
     * <p>예외 상황:
     * <ul>
     *   <li>Base64 디코딩 실패</li>
     *   <li>구분자(:) 누락 또는 형식 오류</li>
     *   <li>숫자 파싱 실패</li>
     * </ul>
     * 위 경우 모두 예외를 외부로 던지지 않고 null을 반환한다.
     */
    public static @Nullable ChatMessageCursorKey decode(@Nullable String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int idx = raw.lastIndexOf(':');
            if (idx <= 0 || idx == raw.length() - 1) {
                return null;
            }
            long ts = Long.parseLong(raw.substring(0, idx));
            long id = Long.parseLong(raw.substring(idx + 1));
            return new ChatMessageCursorKey(ts, id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
