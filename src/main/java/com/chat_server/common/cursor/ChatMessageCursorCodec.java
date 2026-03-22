package com.chat_server.common.cursor;

import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ChatMessageCursorCodec {
    public static String encode(long lastMessageAtEpochMillis, long lastMessageId) {
        String raw = lastMessageAtEpochMillis + ":" + lastMessageId;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

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
