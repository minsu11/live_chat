package com.chat_server.chatmessage.dto.request;

public record ChatSendRequest (Long roomId,
                               String messageType,
                               String text){
}
