package com.chat_server.chatroom.dto.response;

import java.time.LocalDateTime;

public record ChatRoomSummaryResponse(Long roomId,
                                      String chatType,
                                      String title,
                                      String profileUrl,
                                      long memberCount) {
}
