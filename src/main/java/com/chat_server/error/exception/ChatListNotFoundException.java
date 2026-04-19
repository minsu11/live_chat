package com.chat_server.error.exception;

public class ChatListNotFoundException extends RuntimeException {
    public ChatListNotFoundException(String message) {
        super(message);
    }
}
