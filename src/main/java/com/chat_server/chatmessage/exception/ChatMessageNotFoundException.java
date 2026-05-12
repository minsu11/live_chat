package com.chat_server.chatmessage.exception;

import com.chat_server.common.dto.exception.NotFoundException;
import com.chat_server.error.enumulation.ErrorCode;

public class ChatMessageNotFoundException extends NotFoundException {
    public ChatMessageNotFoundException(String message) {
      super(ErrorCode.CHAT_MESSAGE_NOT_FOUND,message);
    }
    public ChatMessageNotFoundException(){
        throw new ChatMessageNotFoundException("chat message not found");
    }
}
