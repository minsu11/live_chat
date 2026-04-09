package com.chat_server.chatmessage.dto.request;

import com.chat_server.chatmessage.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatSendRequest (@NotNull Long roomId,
                               @NotNull MessageType messageType,
                               @NotBlank String messageContent){
}
