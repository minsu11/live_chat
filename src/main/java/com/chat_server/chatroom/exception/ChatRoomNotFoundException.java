package com.chat_server.chatroom.exception;

public class ChatRoomNotFoundException extends RuntimeException {
    public ChatRoomNotFoundException(String message) {
        super(message);
    }

    public ChatRoomNotFoundException() {
        super("chat room not found");
    }
}
