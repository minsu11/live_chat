package com.chat_server.chatmessageread.exception;

public class ChatMessageReadNotFoundException extends RuntimeException {
    public ChatMessageReadNotFoundException() {
        super("chat message read not found");
    }

    public ChatMessageReadNotFoundException(String message) {
        super(message);
    }

}
