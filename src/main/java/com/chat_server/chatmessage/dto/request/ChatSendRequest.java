package com.chat_server.chatmessage.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatSendRequest (@NotBlank Long roomId,
                               String messageType,
                               @NotBlank String messageContent){
}
