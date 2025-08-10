package com.chat_server.common.cursor;

public record ChatListCursorKey(long lastAtEpochMillis, long lastRoomId) {}

