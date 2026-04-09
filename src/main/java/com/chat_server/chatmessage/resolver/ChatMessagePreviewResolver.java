package com.chat_server.chatmessage.resolver;

import com.chat_server.chatmessage.enums.MessageType;


public final class ChatMessagePreviewResolver {
    public static String resolve(MessageType messageType, String messageContent) {

        return switch (messageType) {
            case TEXT, EMOJI -> (messageContent.length() <20 ? messageContent : messageContent.substring(0, 20) + "...");
            case IMAGE -> "사진 보냈습니다.";
            case FILE -> "파일 보냈습니다.";
            case SYSTEM -> "시스템 알림입니다.";
        };
    }
    private ChatMessagePreviewResolver() {}
}
