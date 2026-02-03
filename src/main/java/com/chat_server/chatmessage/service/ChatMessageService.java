package com.chat_server.chatmessage.service;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatroom.entity.ChatRoom;

public interface ChatMessageService {
    ChatMessage createChatMessage(ChatRoom chatRoom, Long userId, String messageType, String text);
}
