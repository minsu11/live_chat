package com.chat_server.chatlist.dto.response;


import java.time.LocalDateTime;

public record ChatRoomListRow(Long roomId, String displayName,
                              LocalDateTime lastMessageAt, LocalDateTime orderAt) {}

