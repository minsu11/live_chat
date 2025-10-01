// src/main/java/com/chat_server/common/cursor/CursorCodec.java
package com.chat_server.common.cursor;

import jakarta.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CursorCodec {

    private CursorCodec() {}

    public static String encode(String lowerName, String lastUuid) {
        String raw = lowerName + ":" + lastUuid;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }


    public static @Nullable CursorKey decode(@Nullable String cursor) {
        if (cursor == null || cursor.isEmpty()) return null;
        String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] parts = decoded.split(":", 2);
        return new CursorKey(parts[0], parts[1]);
    }

}
