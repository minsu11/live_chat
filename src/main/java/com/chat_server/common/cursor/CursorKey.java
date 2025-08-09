package com.chat_server.common.cursor;
/**
 * 커서: "마지막 항목의 lowerName, id" (ex. (bob, 4))
 */
public record CursorKey(String lastLowerName, Long lastId) {

}

