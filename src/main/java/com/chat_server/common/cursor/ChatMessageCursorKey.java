package com.chat_server.common.cursor;

public record ChatMessageCursorKey(long lastMessageAtEpochMillis, long lastMessageId) {
}
