// src/main/java/com/chat_server/common/cursor/CursorCodec.java
package com.chat_server.common.cursor;

import jakarta.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CursorCodec {

    private CursorCodec() {}

    public static String encode(String lowerName, Long id) {
        String raw = lowerName + ":" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static @Nullable CursorKey decode(@Nullable String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int idx = raw.lastIndexOf(':');
            if (idx <= 0 || idx == raw.length() - 1) return null;
            String lowerName = raw.substring(0, idx);
            Long lastId = Long.parseLong(raw.substring(idx + 1));
            return new CursorKey(lowerName, lastId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
